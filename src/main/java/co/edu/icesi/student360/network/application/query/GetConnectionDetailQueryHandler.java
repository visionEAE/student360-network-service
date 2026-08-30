package co.edu.icesi.student360.network.application.query;

import co.edu.icesi.student360.common.api.exception.NotFoundException;
import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.network.application.query.model.ConnectionDetailView;
import co.edu.icesi.student360.network.domain.model.NetworkEdge;
import co.edu.icesi.student360.network.domain.model.PersonProfile;
import co.edu.icesi.student360.network.domain.port.DirectoryClient;
import co.edu.icesi.student360.network.domain.port.SupportNetworkRepository;
import co.edu.icesi.student360.network.domain.service.ConnectionDetailAssembler;
import co.edu.icesi.student360.network.domain.service.StudentNetworkAccessPolicy;
import java.util.List;

/**
 * One person of a student's support network, opened: contact details and a short summary.
 *
 * <p>Reachability is decided by the edges, not by the person node: a person is only visible here if
 * they actually support <em>this</em> student, so knowing a reference is never enough to read
 * somebody's contact card out of the graph.
 */
public class GetConnectionDetailQueryHandler {

  static final String RESOURCE = "Support network connection";

  private final SupportNetworkRepository repository;
  private final DirectoryClient directory;

  public GetConnectionDetailQueryHandler(
      SupportNetworkRepository repository, DirectoryClient directory) {
    this.repository = repository;
    this.directory = directory;
  }

  @Audited(action = "READ_SUPPORT_CONNECTION_DETAIL", subjectType = "STUDENT")
  public ConnectionDetailView handle(GetConnectionDetailQuery query) {
    StudentNetworkAccessPolicy.assertCanRead(query.studentReference());

    List<NetworkEdge> personEdges =
        repository.findEdgesTargeting(query.studentReference()).stream()
            .filter(edge -> edge.personReference().equals(query.personReference()))
            .toList();
    if (personEdges.isEmpty()) {
      throw new NotFoundException(RESOURCE, query.personReference());
    }

    PersonProfile person =
        repository
            .findPerson(query.personReference())
            .orElseThrow(() -> new NotFoundException(RESOURCE, query.personReference()));

    return ConnectionDetailAssembler.assemble(
        query.studentReference(), person, directory.lookup(query.personReference()), personEdges);
  }
}
