package co.edu.icesi.student360.network.domain.port;

import co.edu.icesi.student360.network.domain.model.DirectoryProfile;
import java.util.Optional;

/**
 * Port: the institution's own record of a professor or a student, fetched from core-service on
 * behalf of the current user.
 *
 * <p>Returns empty rather than throwing when the reference names nobody <em>or</em> when
 * core-service cannot be reached: enrichment is additive, so a person's card must still render from
 * what the graph itself stores when the source is down. Callers say so in the view rather than
 * failing the whole read.
 */
public interface DirectoryClient {

  Optional<DirectoryProfile> lookup(String reference);
}
