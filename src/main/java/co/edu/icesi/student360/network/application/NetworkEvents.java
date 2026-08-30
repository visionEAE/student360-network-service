package co.edu.icesi.student360.network.application;

/** Every state change this service makes feeds the warehouse through the outbox. */
public final class NetworkEvents {

  public static final String SUPPORT_CONNECTION_UPSERTED = "SUPPORT_CONNECTION_UPSERTED";
  public static final String SUPPORT_CONNECTION_REMOVED = "SUPPORT_CONNECTION_REMOVED";
  public static final String AGGREGATE_STUDENT = "STUDENT";

  private NetworkEvents() {}
}
