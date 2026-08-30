package co.edu.icesi.student360.network.api.dto;

import co.edu.icesi.student360.network.domain.model.PersonKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * {@code email}, {@code phone} and {@code summary} are meant for people the institution has no
 * record of; for a professor or a fellow student they may be omitted, since core-service's
 * directory is what the read side uses for those.
 */
public record PersonRefRequest(
    String reference,
    PersonKind kind,
    @Size(max = 200) String displayName,
    @Email @Size(max = 200) String email,
    @Size(max = 40) String phone,
    @Size(max = 500) String summary) {}
