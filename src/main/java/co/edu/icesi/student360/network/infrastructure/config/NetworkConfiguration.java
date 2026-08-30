package co.edu.icesi.student360.network.infrastructure.config;

import co.edu.icesi.student360.common.outbox.EventPublisher;
import co.edu.icesi.student360.network.application.command.RemoveConnectionCommandHandler;
import co.edu.icesi.student360.network.application.command.UpsertConnectionCommandHandler;
import co.edu.icesi.student360.network.application.query.GetSupportNetworkQueryHandler;
import co.edu.icesi.student360.network.domain.port.SupportNetworkRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires every CQRS handler; the Neo4j adapter is bound automatically as the only {@link
 * SupportNetworkRepository} bean ({@code @Repository} on the adapter).
 */
@Configuration
public class NetworkConfiguration {

  @Bean
  public UpsertConnectionCommandHandler upsertConnectionCommandHandler(
      SupportNetworkRepository repository, EventPublisher events, Clock clock) {
    return new UpsertConnectionCommandHandler(repository, events, clock);
  }

  @Bean
  public RemoveConnectionCommandHandler removeConnectionCommandHandler(
      SupportNetworkRepository repository, EventPublisher events, Clock clock) {
    return new RemoveConnectionCommandHandler(repository, events, clock);
  }

  @Bean
  public GetSupportNetworkQueryHandler getSupportNetworkQueryHandler(
      SupportNetworkRepository repository) {
    return new GetSupportNetworkQueryHandler(repository);
  }
}
