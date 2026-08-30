package co.edu.icesi.student360.network.infrastructure.persistence;

import co.edu.icesi.student360.network.domain.model.NetworkEdge;
import co.edu.icesi.student360.network.domain.model.PersonContact;
import co.edu.icesi.student360.network.domain.model.PersonKind;
import co.edu.icesi.student360.network.domain.model.PersonProfile;
import co.edu.icesi.student360.network.domain.model.PersonRef;
import co.edu.icesi.student360.network.domain.model.RaterType;
import co.edu.icesi.student360.network.domain.model.RelationshipLabel;
import co.edu.icesi.student360.network.domain.port.SupportNetworkRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

/**
 * The graph adapter: everything the domain needs, in Cypher, behind {@link
 * SupportNetworkRepository}. Uses {@link Neo4jClient} directly rather than Spring Data Neo4j's
 * object-graph mapping, because a {@code SUPPORTS} edge is queried from either direction and two
 * different raters must keep independent edges for the same pair — easier to express and to keep
 * predictable as plain Cypher than to coax out of an object-graph mapper. Timestamps are stored as
 * ISO-8601 strings: simpler than reconciling the driver's temporal types across versions, and this
 * service never needs to query on them.
 */
@Repository
public class Neo4jSupportNetworkRepository implements SupportNetworkRepository {

  private final Neo4jClient client;

  public Neo4jSupportNetworkRepository(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public void upsertConnection(
      String studentReference,
      PersonRef person,
      RelationshipLabel relationshipLabel,
      int weight,
      String note,
      RaterType ratedBy,
      String ratedByReference,
      Instant now) {
    client
        .query(
            """
            MERGE (student:Person {reference: $studentReference})
              ON CREATE SET student.kind = 'STUDENT'
            MERGE (person:Person {reference: $personReference})
              ON CREATE SET person.kind = $personKind, person.displayName = $personDisplayName
              ON MATCH SET
                person.kind = coalesce($personKind, person.kind),
                person.displayName = coalesce($personDisplayName, person.displayName)
            SET person.email = coalesce($personEmail, person.email),
                person.phone = coalesce($personPhone, person.phone),
                person.summary = coalesce($personSummary, person.summary)
            MERGE (person)-[edge:SUPPORTS {ratedByReference: $ratedByReference}]->(student)
              ON CREATE SET edge.createdAt = $now
            SET edge.weight = $weight,
                edge.relationshipLabel = $relationshipLabel,
                edge.ratedBy = $ratedBy,
                edge.note = $note,
                edge.updatedAt = $now
            """)
        .bindAll(
            connectionParams(
                studentReference,
                person,
                relationshipLabel,
                weight,
                note,
                ratedBy,
                ratedByReference,
                now))
        .run();
  }

  @Override
  public void removeConnection(
      String studentReference, String personReference, String ratedByReference) {
    client
        .query(
            """
            MATCH (person:Person {reference: $personReference})
                  -[edge:SUPPORTS {ratedByReference: $ratedByReference}]->
                  (student:Person {reference: $studentReference})
            DELETE edge
            """)
        .bind(studentReference)
        .to("studentReference")
        .bind(personReference)
        .to("personReference")
        .bind(ratedByReference)
        .to("ratedByReference")
        .run();
  }

  @Override
  public Optional<PersonProfile> findPerson(String personReference) {
    return client
        .query(
            """
            MATCH (person:Person {reference: $personReference})
            RETURN person.reference AS reference,
                   person.kind AS kind,
                   person.displayName AS displayName,
                   person.email AS email,
                   person.phone AS phone,
                   person.summary AS summary
            """)
        .bind(personReference)
        .to("personReference")
        .fetch()
        .one()
        .map(Neo4jSupportNetworkRepository::toPersonProfile);
  }

  @Override
  public List<NetworkEdge> findEdgesTargeting(String studentReference) {
    return client
        .query(
            """
            MATCH (person:Person)-[edge:SUPPORTS]->(student:Person {reference: $studentReference})
            RETURN person.reference AS personReference,
                   person.kind AS personKind,
                   person.displayName AS personDisplayName,
                   edge.weight AS weight,
                   edge.relationshipLabel AS relationshipLabel,
                   edge.ratedBy AS ratedBy,
                   edge.ratedByReference AS ratedByReference,
                   edge.note AS note,
                   edge.updatedAt AS updatedAt
            """)
        .bind(studentReference)
        .to("studentReference")
        .fetch()
        .all()
        .stream()
        .map(Neo4jSupportNetworkRepository::toNetworkEdge)
        .toList();
  }

  /**
   * {@link Map#of} rejects {@code null} values, but {@code personKind}, {@code personDisplayName}
   * and {@code note} are all optional (a reused person may be rated without repeating their kind; a
   * note is never required) — a plain, mutable map is what actually allows binding them as null.
   */
  private static Map<String, Object> connectionParams(
      String studentReference,
      PersonRef person,
      RelationshipLabel relationshipLabel,
      int weight,
      String note,
      RaterType ratedBy,
      String ratedByReference,
      Instant now) {
    Map<String, Object> params = new java.util.HashMap<>();
    params.put("studentReference", studentReference);
    params.put("personReference", person.reference());
    params.put("personKind", person.kind() == null ? null : person.kind().name());
    params.put("personDisplayName", person.displayName());
    params.put("personEmail", person.contact().email());
    params.put("personPhone", person.contact().phone());
    params.put("personSummary", person.contact().summary());
    params.put("weight", weight);
    params.put("relationshipLabel", relationshipLabel.name());
    params.put("ratedBy", ratedBy.name());
    params.put("ratedByReference", ratedByReference);
    params.put("note", note);
    params.put("now", now.toString());
    return params;
  }

  private static PersonProfile toPersonProfile(Map<String, Object> row) {
    return new PersonProfile(
        (String) row.get("reference"),
        PersonKind.valueOf((String) row.get("kind")),
        (String) row.get("displayName"),
        new PersonContact(
            (String) row.get("email"), (String) row.get("phone"), (String) row.get("summary")));
  }

  private static NetworkEdge toNetworkEdge(Map<String, Object> row) {
    return new NetworkEdge(
        (String) row.get("personReference"),
        PersonKind.valueOf((String) row.get("personKind")),
        (String) row.get("personDisplayName"),
        ((Number) row.get("weight")).intValue(),
        RelationshipLabel.valueOf((String) row.get("relationshipLabel")),
        RaterType.valueOf((String) row.get("ratedBy")),
        (String) row.get("ratedByReference"),
        (String) row.get("note"),
        Instant.parse((String) row.get("updatedAt")));
  }
}
