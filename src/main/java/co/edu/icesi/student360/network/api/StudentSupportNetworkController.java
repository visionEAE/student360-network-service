package co.edu.icesi.student360.network.api;

import co.edu.icesi.student360.network.api.dto.UpsertConnectionRequest;
import co.edu.icesi.student360.network.api.dto.UpsertConnectionResponse;
import co.edu.icesi.student360.network.application.command.RemoveConnectionCommand;
import co.edu.icesi.student360.network.application.command.RemoveConnectionCommandHandler;
import co.edu.icesi.student360.network.application.command.UpsertConnectionCommand;
import co.edu.icesi.student360.network.application.command.UpsertConnectionCommandHandler;
import co.edu.icesi.student360.network.application.query.GetSupportNetworkQuery;
import co.edu.icesi.student360.network.application.query.GetSupportNetworkQueryHandler;
import co.edu.icesi.student360.network.application.query.model.SupportNetworkView;
import co.edu.icesi.student360.network.domain.model.PersonRef;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A student's own view of, and edits to, who supports them. */
@RestController
@RequestMapping("/api/network/students/{id}")
public class StudentSupportNetworkController {

  private final UpsertConnectionCommandHandler upsert;
  private final RemoveConnectionCommandHandler remove;
  private final GetSupportNetworkQueryHandler getNetwork;

  public StudentSupportNetworkController(
      UpsertConnectionCommandHandler upsert,
      RemoveConnectionCommandHandler remove,
      GetSupportNetworkQueryHandler getNetwork) {
    this.upsert = upsert;
    this.remove = remove;
    this.getNetwork = getNetwork;
  }

  @PostMapping("/connections")
  @ResponseStatus(HttpStatus.CREATED)
  public UpsertConnectionResponse create(
      @PathVariable String id, @Valid @RequestBody UpsertConnectionRequest body) {
    return UpsertConnectionResponse.from(upsert.handle(toCommand(id, null, body)));
  }

  @PatchMapping("/connections/{personReference}")
  public UpsertConnectionResponse update(
      @PathVariable String id,
      @PathVariable String personReference,
      @Valid @RequestBody UpsertConnectionRequest body) {
    return UpsertConnectionResponse.from(upsert.handle(toCommand(id, personReference, body)));
  }

  @DeleteMapping("/connections/{personReference}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable String id, @PathVariable String personReference) {
    remove.handle(new RemoveConnectionCommand(id, personReference));
  }

  @GetMapping("/support-network")
  public SupportNetworkView supportNetwork(@PathVariable String id) {
    return getNetwork.handle(new GetSupportNetworkQuery(id));
  }

  private static UpsertConnectionCommand toCommand(
      String studentId, String targetPersonReference, UpsertConnectionRequest body) {
    PersonRef person =
        body.person() == null
            ? null
            : new PersonRef(
                body.person().reference(), body.person().kind(), body.person().displayName());
    return new UpsertConnectionCommand(
        studentId,
        targetPersonReference,
        person,
        body.relationshipLabel(),
        body.weight(),
        body.note());
  }
}
