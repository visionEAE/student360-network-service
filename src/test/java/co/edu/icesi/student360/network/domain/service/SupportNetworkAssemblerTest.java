package co.edu.icesi.student360.network.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.icesi.student360.network.application.query.model.SupportNetworkView;
import co.edu.icesi.student360.network.domain.model.NetworkEdge;
import co.edu.icesi.student360.network.domain.model.PersonKind;
import co.edu.icesi.student360.network.domain.model.RaterType;
import co.edu.icesi.student360.network.domain.model.RelationshipLabel;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure domain: no Neo4j, no Spring context. */
class SupportNetworkAssemblerTest {

  private static NetworkEdge edge(
      String person, int weight, RaterType ratedBy, String ratedByReference) {
    return new NetworkEdge(
        person,
        PersonKind.FAMILY,
        "Someone",
        weight,
        RelationshipLabel.FAMILY,
        ratedBy,
        ratedByReference,
        null,
        Instant.parse("2026-08-30T00:00:00Z"));
  }

  @Test
  void shouldRankByTheCallersOwnWeightAndPickThePrimarySupport() {
    List<NetworkEdge> edges =
        List.of(
            edge("A-2001", 8, RaterType.SELF, "S-1003"),
            edge("P-mother", 9, RaterType.SELF, "S-1003"));

    SupportNetworkView view = SupportNetworkAssembler.assemble("S-1003", edges, "S-1003");

    assertThat(view.connections()).hasSize(2);
    assertThat(view.connections().get(0).person().reference()).isEqualTo("P-mother");
    assertThat(view.primarySupport().person().reference()).isEqualTo("P-mother");
    assertThat(view.averageWeight()).isEqualTo(8.5);
  }

  @Test
  void shouldFallBackToTheOtherRatersWeightWhenTheCallerHasNotRatedYet() {
    List<NetworkEdge> edges = List.of(edge("A-2001", 6, RaterType.SUPPORT_TEAM, "A-2001"));

    SupportNetworkView view = SupportNetworkAssembler.assemble("S-1003", edges, "S-1003");

    assertThat(view.connections()).hasSize(1);
    assertThat(view.averageWeight()).isEqualTo(6.0);
  }

  @Test
  void shouldKeepTwoRatersEdgesForTheSamePersonSeparate() {
    List<NetworkEdge> edges =
        List.of(
            edge("A-2001", 8, RaterType.SELF, "S-1003"),
            edge("A-2001", 6, RaterType.SUPPORT_TEAM, "A-2001"));

    SupportNetworkView view = SupportNetworkAssembler.assemble("S-1003", edges, "S-1003");

    assertThat(view.connections()).hasSize(1);
    assertThat(view.connections().get(0).edges()).hasSize(2);
    // ranked from the student's own perspective (SELF = 8), not the advisor's note (6)
    assertThat(view.averageWeight()).isEqualTo(8.0);
  }

  @Test
  void shouldReturnAnEmptyNetworkWithNoPrimarySupportWhenThereAreNoEdges() {
    SupportNetworkView view = SupportNetworkAssembler.assemble("S-1003", List.of(), "S-1003");

    assertThat(view.connections()).isEmpty();
    assertThat(view.primarySupport()).isNull();
    assertThat(view.averageWeight()).isNull();
  }
}
