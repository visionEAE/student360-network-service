package co.edu.icesi.student360.network.domain.service;

import co.edu.icesi.student360.network.application.query.model.ConnectionDetailView;
import co.edu.icesi.student360.network.application.query.model.ContactView;
import co.edu.icesi.student360.network.application.query.model.EdgeView;
import co.edu.icesi.student360.network.application.query.model.PersonView;
import co.edu.icesi.student360.network.domain.model.DirectoryProfile;
import co.edu.icesi.student360.network.domain.model.NetworkEdge;
import co.edu.icesi.student360.network.domain.model.PersonProfile;
import java.util.List;
import java.util.Optional;

/**
 * Merges what the graph stores about a person with what the institution's directory knows, into the
 * single card a caller renders. Pure and free of both Neo4j and HTTP, so the precedence rule can be
 * tested on its own.
 *
 * <p>The directory wins whenever it answered: for a professor or a fellow student the SIS is the
 * source of truth for name and email, and a stale value typed into the graph months ago must not
 * shadow it. Anything the directory does not carry — a phone number, a personal note — still comes
 * from the graph, so the two compose rather than one replacing the other wholesale.
 */
public final class ConnectionDetailAssembler {

  private ConnectionDetailAssembler() {}

  public static ConnectionDetailView assemble(
      String studentReference,
      PersonProfile person,
      Optional<DirectoryProfile> directory,
      List<NetworkEdge> personEdges) {
    PersonView personView =
        new PersonView(
            person.reference(),
            person.kind().name(),
            directory
                .map(DirectoryProfile::displayName)
                .filter(name -> !name.isBlank())
                .orElse(person.displayName()));

    List<EdgeView> edges =
        personEdges.stream()
            .map(
                edge ->
                    new EdgeView(
                        edge.weight(),
                        edge.relationshipLabel().name(),
                        edge.ratedBy().name(),
                        edge.updatedAt()))
            .toList();

    return new ConnectionDetailView(
        studentReference, personView, contact(person, directory), edges);
  }

  private static ContactView contact(PersonProfile person, Optional<DirectoryProfile> directory) {
    if (directory.isPresent()) {
      DirectoryProfile profile = directory.get();
      return new ContactView(
          firstNonBlank(profile.email(), person.contact().email()),
          // The directory publishes no phone number; a stored one is still worth showing.
          person.contact().phone(),
          firstNonBlank(profile.detail(), person.contact().summary()),
          profile.headline(),
          ContactView.DIRECTORY);
    }
    if (person.contact().isEmpty()) {
      return ContactView.none();
    }
    return new ContactView(
        person.contact().email(),
        person.contact().phone(),
        person.contact().summary(),
        null,
        ContactView.SELF_REPORTED);
  }

  private static String firstNonBlank(String preferred, String fallback) {
    return preferred != null && !preferred.isBlank() ? preferred : fallback;
  }
}
