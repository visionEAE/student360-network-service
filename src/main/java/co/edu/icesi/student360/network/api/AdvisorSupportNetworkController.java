package co.edu.icesi.student360.network.api;

import co.edu.icesi.student360.network.application.query.GetSupportNetworkQuery;
import co.edu.icesi.student360.network.application.query.GetSupportNetworkQueryHandler;
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

  public AdvisorSupportNetworkController(GetSupportNetworkQueryHandler getNetwork) {
    this.getNetwork = getNetwork;
  }

  @GetMapping("/support-network")
  public SupportNetworkView supportNetwork(@PathVariable String id) {
    return getNetwork.handle(new GetSupportNetworkQuery(id));
  }
}
