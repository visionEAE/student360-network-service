package co.edu.icesi.student360.network.application.query;

public record GetSupportNetworkQuery(String studentReference) {

  @Override
  public String toString() {
    return studentReference;
  }
}
