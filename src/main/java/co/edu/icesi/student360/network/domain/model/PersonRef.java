package co.edu.icesi.student360.network.domain.model;

/**
 * A person to connect to a student: either an existing one, identified by {@code reference} (a
 * cross-service id like {@code A-2001} or {@code PROF-4}, or a network-generated {@code P-<uuid>}),
 * or a brand new one with no {@code reference} yet — the handler generates one before the first
 * write so the caller always gets an id back.
 *
 * <p>{@code contact} is optional and only meaningful for people the institution has no record of;
 * see {@link PersonContact}.
 */
public record PersonRef(
    String reference, PersonKind kind, String displayName, PersonContact contact) {

  public PersonRef {
    contact = contact == null ? PersonContact.EMPTY : contact;
  }

  public PersonRef(String reference, PersonKind kind, String displayName) {
    this(reference, kind, displayName, PersonContact.EMPTY);
  }
}
