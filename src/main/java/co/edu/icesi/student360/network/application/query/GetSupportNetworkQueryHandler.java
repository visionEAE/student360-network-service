package co.edu.icesi.student360.network.application.query;

import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.identity.IdentityContext;
import co.edu.icesi.student360.network.application.query.model.SupportNetworkView;
import co.edu.icesi.student360.network.domain.port.SupportNetworkRepository;
import co.edu.icesi.student360.network.domain.service.StudentNetworkAccessPolicy;
import co.edu.icesi.student360.network.domain.service.SupportNetworkAssembler;

/** "Quién es tu principal red de apoyo": every rated edge pointing at the student, ranked. */
public class GetSupportNetworkQueryHandler {

  private final SupportNetworkRepository repository;

  public GetSupportNetworkQueryHandler(SupportNetworkRepository repository) {
    this.repository = repository;
  }

  @Audited(action = "READ_SUPPORT_NETWORK", subjectType = "STUDENT")
  public SupportNetworkView handle(GetSupportNetworkQuery query) {
    StudentNetworkAccessPolicy.assertCanRead(query.studentReference());
    String callerReference = IdentityContext.require().externalReference();
    return SupportNetworkAssembler.assemble(
        query.studentReference(),
        repository.findEdgesTargeting(query.studentReference()),
        callerReference);
  }
}
