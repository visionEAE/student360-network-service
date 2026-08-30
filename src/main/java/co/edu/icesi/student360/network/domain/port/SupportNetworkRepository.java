package co.edu.icesi.student360.network.domain.port;

import co.edu.icesi.student360.network.domain.model.PersonProfile;
import co.edu.icesi.student360.network.domain.model.PersonRef;
import co.edu.icesi.student360.network.domain.model.RaterType;
import co.edu.icesi.student360.network.domain.model.RelationshipLabel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The graph, behind an interface: the domain never sees Cypher or a Neo4j client. Every method
 * touches only the edges that point <em>at</em> {@code studentReference} — this service's whole job
 * is answering "who supports this student", never a general-purpose graph query.
 */
public interface SupportNetworkRepository {

  /** Creates the person node if {@code person.reference()} is new, then MERGEs one rater's edge. */
  void upsertConnection(
      String studentReference,
      PersonRef person,
      RelationshipLabel relationshipLabel,
      int weight,
      String note,
      RaterType ratedBy,
      String ratedByReference,
      Instant now);

  /** Removes only the edge authored by {@code ratedByReference}; a no-op if none exists. */
  void removeConnection(String studentReference, String personReference, String ratedByReference);

  /** One person node, whatever the graph knows about them; empty if the reference is unknown. */
  Optional<PersonProfile> findPerson(String personReference);

  /** Every edge pointing at the student, one row per (person, rater) pair. */
  List<co.edu.icesi.student360.network.domain.model.NetworkEdge> findEdgesTargeting(
      String studentReference);
}
