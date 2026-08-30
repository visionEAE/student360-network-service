package co.edu.icesi.student360.network.application.command;

import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.audit.RecordType;
import co.edu.icesi.student360.common.outbox.DomainEvent;
import co.edu.icesi.student360.common.outbox.EventPublisher;
import co.edu.icesi.student360.network.application.NetworkEvents;
import co.edu.icesi.student360.network.domain.port.SupportNetworkRepository;
import co.edu.icesi.student360.network.domain.service.StudentNetworkAccessPolicy;
import java.time.Clock;
import java.util.Map;

/** Removes only the caller's own edge toward this person; the other rater's edge is untouched. */
public class RemoveConnectionCommandHandler {

  private final SupportNetworkRepository repository;
  private final EventPublisher events;
  private final Clock clock;

  public RemoveConnectionCommandHandler(
      SupportNetworkRepository repository, EventPublisher events, Clock clock) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  @Audited(
      action = "REMOVE_SUPPORT_CONNECTION",
      subjectType = "STUDENT",
      recordType = RecordType.STATE_CHANGE)
  public void handle(RemoveConnectionCommand command) {
    StudentNetworkAccessPolicy.Rater rater =
        StudentNetworkAccessPolicy.assertCanWrite(command.studentReference());
    repository.removeConnection(
        command.studentReference(), command.personReference(), rater.reference());
    events.publish(
        new DomainEvent(
            NetworkEvents.SUPPORT_CONNECTION_REMOVED,
            NetworkEvents.AGGREGATE_STUDENT,
            command.studentReference(),
            clock.instant(),
            Map.of("personReference", command.personReference(), "ratedBy", rater.type().name())));
  }
}
