package co.edu.icesi.student360.network.application.query.model;

/**
 * @param source {@code DIRECTORY} when core-service resolved an institutional person, {@code
 *     SELF_REPORTED} when the details are the ones whoever added them typed in, {@code NONE} when
 *     nothing is on file — including when core-service was unreachable and the graph held nothing,
 *     so the UI can say "no disponible" rather than imply the person has no contact details.
 */
public record ContactView(
    String email, String phone, String summary, String headline, String source) {

  public static final String DIRECTORY = "DIRECTORY";
  public static final String SELF_REPORTED = "SELF_REPORTED";
  public static final String NONE = "NONE";

  public static ContactView none() {
    return new ContactView(null, null, null, null, NONE);
  }
}
