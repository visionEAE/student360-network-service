package co.edu.icesi.student360.network.application.query;

public record GetConnectionDetailQuery(String studentReference, String personReference) {

  /** The audit aspect records the first argument's string form as the subject id. */
  @Override
  public String toString() {
    return studentReference;
  }
}
