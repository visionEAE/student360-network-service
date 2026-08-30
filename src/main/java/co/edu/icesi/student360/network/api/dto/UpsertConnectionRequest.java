package co.edu.icesi.student360.network.api.dto;

import co.edu.icesi.student360.network.domain.model.RelationshipLabel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertConnectionRequest(
    @Valid PersonRefRequest person,
    @NotNull RelationshipLabel relationshipLabel,
    @NotNull @Min(1) @Max(10) Integer weight,
    @Size(max = 2000) String note) {}
