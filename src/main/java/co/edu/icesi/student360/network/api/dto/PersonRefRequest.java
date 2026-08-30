package co.edu.icesi.student360.network.api.dto;

import co.edu.icesi.student360.network.domain.model.PersonKind;
import jakarta.validation.constraints.Size;

public record PersonRefRequest(
    String reference, PersonKind kind, @Size(max = 200) String displayName) {}
