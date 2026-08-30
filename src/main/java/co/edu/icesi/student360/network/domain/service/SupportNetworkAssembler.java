package co.edu.icesi.student360.network.domain.service;

import co.edu.icesi.student360.network.application.query.model.ConnectionView;
import co.edu.icesi.student360.network.application.query.model.EdgeView;
import co.edu.icesi.student360.network.application.query.model.PersonView;
import co.edu.icesi.student360.network.application.query.model.SupportNetworkView;
import co.edu.icesi.student360.network.domain.model.NetworkEdge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the flat rows the graph returns (one per person, per rater) into the ranked network view a
 * caller sees. Pure and Neo4j-free by design, so the ranking rule can be tested without a database:
 * group by the other person, then rank each connection by <em>the caller's own rating of it</em>
 * when one exists, falling back to whichever other rating is on file — a caller who has not rated a
 * relationship yet still sees it, just not ranked by their own opinion.
 */
public final class SupportNetworkAssembler {

  private SupportNetworkAssembler() {}

  public static SupportNetworkView assemble(
      String studentReference, List<NetworkEdge> edges, String callerReference) {
    Map<String, List<NetworkEdge>> byPerson = new LinkedHashMap<>();
    for (NetworkEdge edge : edges) {
      byPerson.computeIfAbsent(edge.personReference(), key -> new ArrayList<>()).add(edge);
    }

    record Ranked(ConnectionView view, int rankWeight) {}

    List<Ranked> ranked =
        byPerson.values().stream()
            .map(
                personEdges -> {
                  ConnectionView view = toView(personEdges);
                  int weight = rankWeight(personEdges, callerReference);
                  return new Ranked(view, weight);
                })
            .sorted(Comparator.comparingInt(Ranked::rankWeight).reversed())
            .toList();

    List<ConnectionView> connections = ranked.stream().map(Ranked::view).toList();
    ConnectionView primarySupport = connections.isEmpty() ? null : connections.get(0);
    Double averageWeight =
        ranked.isEmpty() ? null : ranked.stream().mapToInt(Ranked::rankWeight).average().orElse(0);

    return new SupportNetworkView(studentReference, connections, primarySupport, averageWeight);
  }

  /** The caller's own edge for this person if they have rated it; otherwise the first on file. */
  private static int rankWeight(List<NetworkEdge> personEdges, String callerReference) {
    return personEdges.stream()
        .filter(edge -> edge.ratedByReference().equals(callerReference))
        .findFirst()
        .or(() -> personEdges.stream().findFirst())
        .map(NetworkEdge::weight)
        .orElse(0);
  }

  private static ConnectionView toView(List<NetworkEdge> personEdges) {
    NetworkEdge first = personEdges.get(0);
    PersonView person =
        new PersonView(
            first.personReference(), first.personKind().name(), first.personDisplayName());
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
    return new ConnectionView(person, edges);
  }
}
