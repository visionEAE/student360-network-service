package co.edu.icesi.student360.network.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.icesi.student360.network.application.query.model.ConnectionDetailView;
import co.edu.icesi.student360.network.application.query.model.ContactView;
import co.edu.icesi.student360.network.domain.model.DirectoryProfile;
import co.edu.icesi.student360.network.domain.model.NetworkEdge;
import co.edu.icesi.student360.network.domain.model.PersonContact;
import co.edu.icesi.student360.network.domain.model.PersonKind;
import co.edu.icesi.student360.network.domain.model.PersonProfile;
import co.edu.icesi.student360.network.domain.model.RaterType;
import co.edu.icesi.student360.network.domain.model.RelationshipLabel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConnectionDetailAssemblerTest {

  private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

  @Test
  void shouldPreferTheDirectoryOverStoredValuesForAnInstitutionalPerson() {
    PersonProfile stored =
        new PersonProfile(
            "PROF-4",
            PersonKind.PROFESSOR,
            "Lucia F.",
            new PersonContact("stale@old.example", "+57 300 000 0000", "apunte viejo"));
    DirectoryProfile directory =
        new DirectoryProfile(
            "PROF-4",
            "Dra. Lucía Fernández",
            "lucia.fernandez@icesi.edu.co",
            "Psychology",
            "Psicopatología");

    ConnectionDetailView view =
        ConnectionDetailAssembler.assemble(
            "S-1003", stored, Optional.of(directory), List.of(edge("PROF-4")));

    assertThat(view.person().displayName()).isEqualTo("Dra. Lucía Fernández");
    assertThat(view.contact().email()).isEqualTo("lucia.fernandez@icesi.edu.co");
    assertThat(view.contact().headline()).isEqualTo("Psychology");
    assertThat(view.contact().summary()).isEqualTo("Psicopatología");
    assertThat(view.contact().source()).isEqualTo(ContactView.DIRECTORY);
    // The directory publishes no phone, so the stored one still shows rather than being dropped.
    assertThat(view.contact().phone()).isEqualTo("+57 300 000 0000");
  }

  @Test
  void shouldFallBackToStoredContactWhenTheDirectoryHasNothing() {
    PersonProfile stored =
        new PersonProfile(
            "P-1",
            PersonKind.FAMILY,
            "Marta Rojas (madre)",
            new PersonContact("marta@example.com", "+57 300 111 2233", "Mi mamá, vive en Cali"));

    ConnectionDetailView view =
        ConnectionDetailAssembler.assemble(
            "S-1003", stored, Optional.empty(), List.of(edge("P-1")));

    assertThat(view.person().displayName()).isEqualTo("Marta Rojas (madre)");
    assertThat(view.contact().email()).isEqualTo("marta@example.com");
    assertThat(view.contact().phone()).isEqualTo("+57 300 111 2233");
    assertThat(view.contact().summary()).isEqualTo("Mi mamá, vive en Cali");
    assertThat(view.contact().source()).isEqualTo(ContactView.SELF_REPORTED);
  }

  @Test
  void shouldReportNoContactRatherThanEmptyStringsWhenNothingIsOnFile() {
    PersonProfile stored =
        new PersonProfile("P-2", PersonKind.PEER, "Alguien", PersonContact.EMPTY);

    ConnectionDetailView view =
        ConnectionDetailAssembler.assemble(
            "S-1003", stored, Optional.empty(), List.of(edge("P-2")));

    assertThat(view.contact().source()).isEqualTo(ContactView.NONE);
    assertThat(view.contact().email()).isNull();
    assertThat(view.contact().phone()).isNull();
  }

  @Test
  void shouldCarryEveryRatersEdge() {
    PersonProfile stored = new PersonProfile("P-3", PersonKind.FAMILY, "Papá", PersonContact.EMPTY);

    ConnectionDetailView view =
        ConnectionDetailAssembler.assemble(
            "S-1003",
            stored,
            Optional.empty(),
            List.of(edge("P-3"), edgeRatedBy("P-3", RaterType.SUPPORT_TEAM, "A-2001", 6)));

    assertThat(view.edges()).hasSize(2);
    assertThat(view.edges()).extracting("ratedBy").containsExactly("SELF", "SUPPORT_TEAM");
  }

  private static NetworkEdge edge(String personReference) {
    return edgeRatedBy(personReference, RaterType.SELF, "S-1003", 9);
  }

  private static NetworkEdge edgeRatedBy(
      String personReference, RaterType ratedBy, String ratedByReference, int weight) {
    return new NetworkEdge(
        personReference,
        PersonKind.FAMILY,
        "ignored",
        weight,
        RelationshipLabel.FAMILY,
        ratedBy,
        ratedByReference,
        null,
        NOW);
  }
}
