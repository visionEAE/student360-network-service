package co.edu.icesi.student360.network.application.command;

public record RemoveConnectionCommand(String studentReference, String personReference) {

  @Override
  public String toString() {
    return studentReference;
  }
}
