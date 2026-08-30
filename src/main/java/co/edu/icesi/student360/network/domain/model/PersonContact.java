package co.edu.icesi.student360.network.domain.model;

/**
 * How to reach someone in a support network, and one line about who they are.
 *
 * <p>Distinct from a {@code SUPPORTS} edge's {@code note}: a note is one rater's private remark
 * about the relationship, never shown to the other rater, whereas this describes the
 * <em>person</em> and is shown to everyone who may read the network — the student and their support
 * team.
 *
 * <p>For a person the institution has no record of (family, a friend outside the university, an
 * external counsellor) these are typed in by whoever added them and stored on the graph node. For a
 * professor or a fellow student they are left null here and resolved from core-service's directory
 * at read time instead, so the SIS stays the one source of truth for institutional contact details.
 */
public record PersonContact(String email, String phone, String summary) {

  public static final PersonContact EMPTY = new PersonContact(null, null, null);

  public boolean isEmpty() {
    return email == null && phone == null && summary == null;
  }
}
