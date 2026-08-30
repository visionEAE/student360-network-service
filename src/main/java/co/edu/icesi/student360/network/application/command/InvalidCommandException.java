package co.edu.icesi.student360.network.application.command;

import co.edu.icesi.student360.common.api.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidCommandException extends DomainException {

  public InvalidCommandException(String detail) {
    super(HttpStatus.BAD_REQUEST, "Invalid request", detail);
  }
}
