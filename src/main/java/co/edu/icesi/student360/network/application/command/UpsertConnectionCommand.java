package co.edu.icesi.student360.network.application.command;

import co.edu.icesi.student360.network.domain.model.PersonRef;
import co.edu.icesi.student360.network.domain.model.RelationshipLabel;

/**
 * {@code targetPersonReference} is set only when the caller is updating a specific, already
 * identified connection (the {@code PATCH} path); {@code null} means "resolve or create the person
 * from {@code person}" (the {@code POST} path).
 */
public record UpsertConnectionCommand(
    String studentReference,
    String targetPersonReference,
    PersonRef person,
    RelationshipLabel relationshipLabel,
    Integer weight,
    String note) {

  @Override
  public String toString() {
    return studentReference;
  }
}
