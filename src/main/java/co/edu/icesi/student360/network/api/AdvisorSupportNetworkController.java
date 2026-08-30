package co.edu.icesi.student360.network.api;

import co.edu.icesi.student360.network.application.query.GetConnectionDetailQuery;
import co.edu.icesi.student360.network.application.query.GetConnectionDetailQueryHandler;
import co.edu.icesi.student360.network.application.query.GetSupportNetworkQuery;
import co.edu.icesi.student360.network.application.query.GetSupportNetworkQueryHandler;
import co.edu.icesi.student360.network.application.query.model.ConnectionDetailView;
import co.edu.icesi.student360.network.application.query.model.SupportNetworkView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Same read model as the student's own view, from the advisor's side. */
@RestController
@RequestMapping("/api/network/advisors/me/students/{id}")
public class AdvisorSupportNetworkController {

  private final GetSupportNetworkQueryHandler getNetwork;
  private final GetConnectionDetailQueryHandler getDetail;

  public AdvisorSupportNetworkController(
      GetSupportNetworkQueryHandler getNetwork, GetConnectionDetailQueryHandler getDetail) {
    this.getNetwork = getNetwork;
    this.getDetail = getDetail;
  }

  @GetMapping("/support-network")
  public SupportNetworkView supportNetwork(@PathVariable String id) {
    return getNetwork.handle(new GetSupportNetworkQuery(id));
  }

  @GetMapping("/connections/{personReference}")
  public ConnectionDetailView connection(
      @PathVariable String id, @PathVariable String personReference) {
    return getDetail.handle(new GetConnectionDetailQuery(id, personReference));
  }
}
