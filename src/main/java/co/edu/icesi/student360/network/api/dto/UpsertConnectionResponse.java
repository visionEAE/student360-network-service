package co.edu.icesi.student360.network.api.dto;

import co.edu.icesi.student360.network.application.command.UpsertConnectionResult;

public record UpsertConnectionResponse(String personReference, int weight) {

  public static UpsertConnectionResponse from(UpsertConnectionResult result) {
    return new UpsertConnectionResponse(result.personReference(), result.weight());
  }
}
