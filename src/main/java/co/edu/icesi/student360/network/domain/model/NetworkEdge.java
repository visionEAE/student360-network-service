package co.edu.icesi.student360.network.domain.model;

import java.time.Instant;

/**
 * One {@code SUPPORTS} edge pointing at a student, as read back from the graph: the person on the
 * other end, and the rating one specific rater gave it. A person may appear more than once for the
 * same student — once per rater — which is exactly what lets a student's own opinion and the
 * support team's separately-kept note about the same relationship coexist.
 */
public record NetworkEdge(
    String personReference,
    PersonKind personKind,
    String personDisplayName,
    int weight,
    RelationshipLabel relationshipLabel,
    RaterType ratedBy,
    String ratedByReference,
    String note,
    Instant updatedAt) {}
