package co.edu.icesi.student360.network.application.command;

import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.audit.RecordType;
import co.edu.icesi.student360.common.outbox.DomainEvent;
import co.edu.icesi.student360.common.outbox.EventPublisher;
import co.edu.icesi.student360.network.application.NetworkEvents;
import co.edu.icesi.student360.network.domain.model.PersonContact;
import co.edu.icesi.student360.network.domain.model.PersonRef;
import co.edu.icesi.student360.network.domain.port.SupportNetworkRepository;
import co.edu.icesi.student360.network.domain.service.StudentNetworkAccessPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A student rating their own incoming support, or an advisor noting a {@code SUPPORT_TEAM}-tagged
 * connection for a student. Both the {@code POST} (new or reused person) and {@code PATCH}
 * (identified person) API paths funnel through this one handler.
 */
public class UpsertConnectionCommandHandler {

  private final SupportNetworkRepository repository;
  private final EventPublisher events;
  private final Clock clock;

  public UpsertConnectionCommandHandler(
      SupportNetworkRepository repository, EventPublisher events, Clock clock) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  @Audited(
      action = "UPSERT_SUPPORT_CONNECTION",
      subjectType = "STUDENT",
      recordType = RecordType.STATE_CHANGE)
  public UpsertConnectionResult handle(UpsertConnectionCommand command) {
    StudentNetworkAccessPolicy.Rater rater =
        StudentNetworkAccessPolicy.assertCanWrite(command.studentReference());
    int weight = requireValidWeight(command.weight());
    if (command.relationshipLabel() == null) {
      throw new InvalidCommandException("relationshipLabel is required");
    }
    PersonRef person = resolvePerson(command);

    Instant now = clock.instant();
    repository.upsertConnection(
        command.studentReference(),
        person,
        command.relationshipLabel(),
        weight,
        command.note(),
        rater.type(),
        rater.reference(),
        now);

    events.publish(
        new DomainEvent(
            NetworkEvents.SUPPORT_CONNECTION_UPSERTED,
            NetworkEvents.AGGREGATE_STUDENT,
            command.studentReference(),
            now,
            Map.of(
                "personReference", person.reference(),
                "relationshipLabel", command.relationshipLabel().name(),
                "weight", weight,
                "ratedBy", rater.type().name())));
    return new UpsertConnectionResult(person.reference(), weight);
  }

  private static int requireValidWeight(Integer weight) {
    if (weight == null || weight < 1 || weight > 10) {
      throw new InvalidCommandException("weight must be between 1 and 10");
    }
    return weight;
  }

  private static PersonRef resolvePerson(UpsertConnectionCommand command) {
    // PATCH identifies the person by path; the body only refines their kind/displayName/contact.
    if (command.targetPersonReference() != null) {
      PersonRef body = command.person();
      return new PersonRef(
          command.targetPersonReference(),
          body == null ? null : body.kind(),
          body == null ? null : body.displayName(),
          body == null ? PersonContact.EMPTY : body.contact());
    }
    PersonRef person = command.person();
    if (person == null || person.kind() == null) {
      throw new InvalidCommandException("person.kind is required");
    }
    if (person.reference() != null) {
      return person;
    }
    boolean hasName = person.displayName() != null && !person.displayName().isBlank();
    if (!hasName) {
      throw new InvalidCommandException("person.displayName is required for a new person");
    }
    return new PersonRef(
        "P-" + UUID.randomUUID(), person.kind(), person.displayName(), person.contact());
  }
}
