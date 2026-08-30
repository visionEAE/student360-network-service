package co.edu.icesi.student360.network.infrastructure.client;

import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;
import co.edu.icesi.student360.common.identity.IdentityHeaders;
import co.edu.icesi.student360.common.logging.Correlation;
import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;

/**
 * What every outbound call carries: a service token for the target audience (this service proves
 * who it is), the user identity on whose behalf the call is made (so the target applies its own
 * fine-grained rule and audits the real actor), and the request id (so the audit trail of one
 * request spans the services it touched).
 */
public class DownstreamRequestInterceptor implements RequestInterceptor {

  private final ServiceTokenProvider serviceTokens;
  private final String audience;

  public DownstreamRequestInterceptor(ServiceTokenProvider serviceTokens, String audience) {
    this.serviceTokens = serviceTokens;
    this.audience = audience;
  }

  @Override
  public void apply(RequestTemplate template) {
    template.header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceTokens.tokenFor(audience));
    Correlation.currentRequestId()
        .ifPresent(id -> template.header(Correlation.REQUEST_ID_HEADER, id));
    IdentityContext.current().ifPresent(identity -> propagate(template, identity));
  }

  private static void propagate(RequestTemplate template, Identity identity) {
    template.header(IdentityHeaders.USER_ID, identity.userId().toString());
    template.header(IdentityHeaders.USER_ROLES, String.join(",", identity.roles()));
    if (identity.externalReference() != null) {
      template.header(IdentityHeaders.EXTERNAL_REFERENCE, identity.externalReference());
    }
  }
}
