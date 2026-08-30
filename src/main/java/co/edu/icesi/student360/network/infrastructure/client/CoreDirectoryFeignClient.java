package co.edu.icesi.student360.network.infrastructure.client;

import co.edu.icesi.student360.network.domain.model.DirectoryProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "core-service",
    url = "${student360.clients.core-service.url}",
    configuration = CoreDirectoryFeignClient.Configuration.class)
public interface CoreDirectoryFeignClient {

  @GetMapping("/api/core/directory/{reference}")
  DirectoryProfile profile(@PathVariable("reference") String reference);

  class Configuration {
    @org.springframework.context.annotation.Bean
    DownstreamRequestInterceptor coreServiceRequestInterceptor(
        co.edu.icesi.student360.common.security.ServiceTokenProvider serviceTokens) {
      return new DownstreamRequestInterceptor(serviceTokens, "core-service");
    }
  }
}
