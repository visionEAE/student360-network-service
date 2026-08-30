package co.edu.icesi.student360.network.application.query.model;

import java.time.Instant;

public record EdgeView(int weight, String relationshipLabel, String ratedBy, Instant updatedAt) {}
