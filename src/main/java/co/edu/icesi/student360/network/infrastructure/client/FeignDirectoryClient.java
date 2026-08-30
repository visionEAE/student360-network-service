package co.edu.icesi.student360.network.infrastructure.client;

import co.edu.icesi.student360.network.domain.model.DirectoryProfile;
import co.edu.icesi.student360.network.domain.port.DirectoryClient;
import feign.FeignException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter: only references core-service can actually resolve are asked for, and any failure — an
 * unknown reference, a 5xx, a timeout — degrades to empty so a person's card still renders from the
 * graph's own data. Logged at debug, not warn: a family member having no SIS record is the normal
 * case here, not an incident.
 */
@Component
public class FeignDirectoryClient implements DirectoryClient {

  private static final Logger log = LoggerFactory.getLogger(FeignDirectoryClient.class);
  private static final String STUDENT_PREFIX = "S-";
  private static final String PROFESSOR_PREFIX = "PROF-";

  private final CoreDirectoryFeignClient feign;

  public FeignDirectoryClient(CoreDirectoryFeignClient feign) {
    this.feign = feign;
  }

  @Override
  public Optional<DirectoryProfile> lookup(String reference) {
    if (!isInstitutional(reference)) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(feign.profile(reference));
    } catch (FeignException exception) {
      log.debug("core-service could not resolve directory reference {}", reference, exception);
      return Optional.empty();
    }
  }

  /**
   * Only a student or professor reference means anything to core-service; a network-generated
   * {@code P-<uuid>} or an advisor's {@code A-*} is skipped rather than sent and 404ed.
   */
  private static boolean isInstitutional(String reference) {
    return reference != null
        && (reference.startsWith(STUDENT_PREFIX) || reference.startsWith(PROFESSOR_PREFIX));
  }
}
