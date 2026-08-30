package co.edu.icesi.student360.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.icesi.student360.common.identity.IdentityHeaders;
import co.edu.icesi.student360.common.logging.Correlation;
import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import co.edu.icesi.student360.network.domain.model.DirectoryProfile;
import co.edu.icesi.student360.network.domain.port.DirectoryClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A student rates a new family member and their assigned advisor; the advisor separately notes a
 * connection for the same student; both raters' edges coexist; only the caller's own edge is
 * removable; a student may not write another student's network; an out-of-range weight is a 400.
 */
@SpringBootTest(
    properties = {
      "NETWORK_DB_PASSWORD=unused-overridden-by-testcontainers",
      "SERVICE_TOKEN_SECRET=" + SupportNetworkFlowIntegrationTest.SECRET
    })
@AutoConfigureMockMvc
@Testcontainers
class SupportNetworkFlowIntegrationTest {

  static final String SECRET = "0123456789abcdef0123456789abcdef-test-only";

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16").withInitScript("db/test-init.sql");

  @Container @ServiceConnection
  static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5-community");

  private static final UUID MARIA = UUID.fromString("11111111-1111-1111-1111-000000001003");
  private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-000000001001");
  private static final UUID CARLOS = UUID.fromString("22222222-2222-2222-2222-000000002001");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ServiceTokenProvider tokens;
  @Autowired private ObjectMapper json;
  @Autowired private org.springframework.data.neo4j.core.Neo4jClient neo4jClient;

  /**
   * core-service is not running in this test; mocking the port keeps both the enriched and the
   * degraded path deterministic rather than depending on a real call failing in a particular way.
   */
  @MockitoBean private DirectoryClient directoryClient;

  @BeforeEach
  void cleanAuditTrailAndGraph() {
    jdbc.update("DELETE FROM audit.audit_record");
    // Unlike the Postgres-backed audit trail, the graph is not scoped per test by any migration —
    // the container is shared across the whole class, so each test starts from an empty graph.
    neo4jClient.query("MATCH (n) DETACH DELETE n").run();
  }

  @Test
  void shouldRankConnectionsByTheStudentsOwnWeightAndExposePrimarySupport() throws Exception {
    String motherBody =
        """
        {"person":{"kind":"FAMILY","displayName":"Marta Rojas (madre)"},
         "relationshipLabel":"FAMILY","weight":9,"note":"siempre disponible"}""";
    String motherResponse =
        mockMvc
            .perform(
                as(
                        MARIA,
                        "STUDENT",
                        "S-1003",
                        post("/api/network/students/S-1003/connections"),
                        "net-mother")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(motherBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.weight").value(9))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String motherReference = json.readTree(motherResponse).path("personReference").asText();
    assertThat(motherReference).startsWith("P-");

    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    post("/api/network/students/S-1003/connections"),
                    "net-advisor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"reference":"A-2001","kind":"ADVISOR"},
                     "relationshipLabel":"ADVISOR","weight":7}"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.personReference").value("A-2001"));

    String networkBody =
        mockMvc
            .perform(
                as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    get("/api/network/students/S-1003/support-network"),
                    "net-read"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode network = json.readTree(networkBody);
    assertThat(network.path("connections")).hasSize(2);
    assertThat(network.path("primarySupport").path("person").path("reference").asText())
        .isEqualTo(motherReference);
    assertThat(network.path("primarySupport").path("person").path("kind").asText())
        .isEqualTo("FAMILY");
    assertThat(network.path("primarySupport").path("edges").get(0).path("weight").asInt())
        .isEqualTo(9);
    assertThat(network.path("averageWeight").asDouble()).isEqualTo(8.0);

    Map<String, Object> audit =
        jdbc.queryForList("SELECT * FROM audit.audit_record WHERE action = 'READ_SUPPORT_NETWORK'")
            .get(0);
    assertThat(audit)
        .containsEntry("outcome", "ALLOWED")
        .containsEntry("authorization_basis", "SELF");
  }

  @Test
  void shouldKeepTheAdvisorsNoteSeparateFromTheStudentsOwnRatingOfTheSamePerson() throws Exception {
    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    post("/api/network/students/S-1003/connections"),
                    "net-self")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"reference":"A-2001","kind":"ADVISOR"},
                     "relationshipLabel":"ADVISOR","weight":8}"""))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            as(
                    CARLOS,
                    "ADVISOR",
                    "A-2001",
                    post("/api/network/students/S-1003/connections"),
                    "net-team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"reference":"A-2001","kind":"ADVISOR"},
                     "relationshipLabel":"MENTOR","weight":6,"note":"weekly check-ins"}"""))
        .andExpect(status().isCreated());

    String body =
        mockMvc
            .perform(
                as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    get("/api/network/students/S-1003/support-network"),
                    "net-both"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode connections = json.readTree(body).path("connections");
    assertThat(connections).hasSize(1);
    assertThat(connections.get(0).path("edges")).hasSize(2);
    List<String> raters =
        json.convertValue(connections.get(0).path("edges"), List.class).stream()
            .map(e -> ((Map<?, ?>) e).get("ratedBy").toString())
            .toList();
    assertThat(raters).containsExactlyInAnyOrder("SELF", "SUPPORT_TEAM");
  }

  @Test
  void shouldUpdateOnlyTheCallersOwnEdgeAndRemoveOnlyTheCallersOwnEdge() throws Exception {
    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    post("/api/network/students/S-1003/connections"),
                    "net-create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"reference":"A-2001","kind":"ADVISOR"},
                     "relationshipLabel":"ADVISOR","weight":5}"""))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    patch("/api/network/students/S-1003/connections/A-2001"),
                    "net-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"relationshipLabel":"ADVISOR","weight":10}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weight").value(10));

    mockMvc
        .perform(
            as(
                    CARLOS,
                    "ADVISOR",
                    "A-2001",
                    patch("/api/network/students/S-1003/connections/A-2001"),
                    "net-team-add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"relationshipLabel":"MENTOR","weight":4}"""))
        .andExpect(status().isCreated().equals(status()) ? status().isCreated() : status().isOk());

    mockMvc
        .perform(
            as(
                MARIA,
                "STUDENT",
                "S-1003",
                delete("/api/network/students/S-1003/connections/A-2001"),
                "net-delete"))
        .andExpect(status().isNoContent());

    String remaining =
        mockMvc
            .perform(
                as(
                    CARLOS,
                    "ADVISOR",
                    "A-2001",
                    get("/api/network/advisors/me/students/S-1003/support-network"),
                    "net-after-delete"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode connections = json.readTree(remaining).path("connections");
    assertThat(connections).hasSize(1);
    assertThat(connections.get(0).path("edges")).hasSize(1);
    assertThat(connections.get(0).path("edges").get(0).path("ratedBy").asText())
        .isEqualTo("SUPPORT_TEAM");
  }

  @Test
  void shouldRefuseAStudentWritingAnotherStudentsNetworkAndAuditTheDenial() throws Exception {
    mockMvc
        .perform(
            as(
                    ANA,
                    "STUDENT",
                    "S-1001",
                    post("/api/network/students/S-1003/connections"),
                    "net-forbidden")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"kind":"PEER","displayName":"Someone"},
                     "relationshipLabel":"PEER","weight":5}"""))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.requestId").value("net-forbidden"));

    Map<String, Object> audit =
        jdbc.queryForMap("SELECT outcome, authorization_basis FROM audit.audit_record");
    assertThat(audit)
        .containsEntry("outcome", "DENIED")
        .containsEntry("authorization_basis", "NONE");
  }

  @Test
  void shouldRejectAnOutOfRangeWeight() throws Exception {
    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    post("/api/network/students/S-1003/connections"),
                    "net-bad-weight")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"kind":"PEER","displayName":"Someone"},
                     "relationshipLabel":"PEER","weight":11}"""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectCallsWithoutServiceTokenBeforeReachingTheDomain() throws Exception {
    mockMvc
        .perform(
            get("/api/network/students/S-1003/support-network")
                .header(IdentityHeaders.USER_ID, MARIA.toString())
                .header(IdentityHeaders.USER_ROLES, "STUDENT")
                .header(IdentityHeaders.EXTERNAL_REFERENCE, "S-1003"))
        .andExpect(status().isUnauthorized());
    assertThat(jdbc.queryForList("SELECT * FROM audit.audit_record")).isEmpty();
  }

  @Test
  void shouldOpenAPersonalConnectionWithTheContactDetailsTheStudentTypedIn() throws Exception {
    String created =
        mockMvc
            .perform(
                as(
                        MARIA,
                        "STUDENT",
                        "S-1003",
                        post("/api/network/students/S-1003/connections"),
                        "detail-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"person":{"kind":"FAMILY","displayName":"Marta Rojas (madre)",
                                   "email":"marta.rojas@example.com","phone":"+57 300 111 2233",
                                   "summary":"Mi mamá, vive en Cali"},
                         "relationshipLabel":"FAMILY","weight":9}"""))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String motherReference = json.readTree(created).path("personReference").asText();

    mockMvc
        .perform(
            as(
                MARIA,
                "STUDENT",
                "S-1003",
                get("/api/network/students/S-1003/connections/" + motherReference),
                "detail-read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.person.displayName").value("Marta Rojas (madre)"))
        .andExpect(jsonPath("$.contact.email").value("marta.rojas@example.com"))
        .andExpect(jsonPath("$.contact.phone").value("+57 300 111 2233"))
        .andExpect(jsonPath("$.contact.summary").value("Mi mamá, vive en Cali"))
        .andExpect(jsonPath("$.contact.source").value("SELF_REPORTED"))
        .andExpect(jsonPath("$.edges.length()").value(1))
        .andExpect(jsonPath("$.edges[0].weight").value(9));

    assertThat(auditActions()).contains("READ_SUPPORT_CONNECTION_DETAIL");
  }

  @Test
  void shouldEnrichAProfessorFromTheDirectoryInsteadOfTheStoredName() throws Exception {
    org.mockito.Mockito.when(directoryClient.lookup("PROF-4"))
        .thenReturn(
            Optional.of(
                new DirectoryProfile(
                    "PROF-4",
                    "Dra. Lucía Fernández",
                    "lucia.fernandez@icesi.edu.co",
                    "Psychology",
                    "Psicopatología")));

    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    post("/api/network/students/S-1003/connections"),
                    "detail-prof-create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"reference":"PROF-4","kind":"PROFESSOR","displayName":"Lucia F."},
                     "relationshipLabel":"PROFESSOR","weight":6}"""))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            as(
                MARIA,
                "STUDENT",
                "S-1003",
                get("/api/network/students/S-1003/connections/PROF-4"),
                "detail-prof-read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.person.displayName").value("Dra. Lucía Fernández"))
        .andExpect(jsonPath("$.contact.email").value("lucia.fernandez@icesi.edu.co"))
        .andExpect(jsonPath("$.contact.headline").value("Psychology"))
        .andExpect(jsonPath("$.contact.summary").value("Psicopatología"))
        .andExpect(jsonPath("$.contact.source").value("DIRECTORY"));
  }

  @Test
  void shouldStillRenderTheCardWhenTheDirectoryIsUnavailable() throws Exception {
    // The port degrades to empty when core-service cannot be reached; the card must survive it.
    org.mockito.Mockito.when(directoryClient.lookup("PROF-4")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            as(
                    MARIA,
                    "STUDENT",
                    "S-1003",
                    post("/api/network/students/S-1003/connections"),
                    "detail-degraded-create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"person":{"reference":"PROF-4","kind":"PROFESSOR",
                               "displayName":"Dra. Lucía Fernández"},
                     "relationshipLabel":"PROFESSOR","weight":6}"""))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            as(
                MARIA,
                "STUDENT",
                "S-1003",
                get("/api/network/students/S-1003/connections/PROF-4"),
                "detail-degraded-read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.person.displayName").value("Dra. Lucía Fernández"))
        .andExpect(jsonPath("$.contact.source").value("NONE"));
  }

  @Test
  void shouldNotRevealAPersonWhoDoesNotSupportThisStudent() throws Exception {
    // The person exists in the graph, but only as Ana's connection — not María's.
    String created =
        mockMvc
            .perform(
                as(
                        ANA,
                        "STUDENT",
                        "S-1001",
                        post("/api/network/students/S-1001/connections"),
                        "detail-foreign-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"person":{"kind":"FAMILY","displayName":"Alguien de Ana",
                                   "phone":"+57 300 999 8877"},
                         "relationshipLabel":"FAMILY","weight":8}"""))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String anasPerson = json.readTree(created).path("personReference").asText();

    mockMvc
        .perform(
            as(
                MARIA,
                "STUDENT",
                "S-1003",
                get("/api/network/students/S-1003/connections/" + anasPerson),
                "detail-foreign-read"))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldLetAnAdvisorOpenTheSameCard() throws Exception {
    String created =
        mockMvc
            .perform(
                as(
                        MARIA,
                        "STUDENT",
                        "S-1003",
                        post("/api/network/students/S-1003/connections"),
                        "detail-advisor-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"person":{"kind":"FAMILY","displayName":"Marta Rojas (madre)",
                                   "phone":"+57 300 111 2233"},
                         "relationshipLabel":"FAMILY","weight":9}"""))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String motherReference = json.readTree(created).path("personReference").asText();

    mockMvc
        .perform(
            as(
                CARLOS,
                "ADVISOR",
                "A-2001",
                get("/api/network/advisors/me/students/S-1003/connections/" + motherReference),
                "detail-advisor-read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contact.phone").value("+57 300 111 2233"))
        .andExpect(jsonPath("$.person.displayName").value("Marta Rojas (madre)"));
  }

  private List<String> auditActions() {
    return jdbc.queryForList("SELECT action FROM audit.audit_record").stream()
        .map(row -> (String) row.get("action"))
        .toList();
  }

  private MockHttpServletRequestBuilder as(
      UUID userId,
      String role,
      String reference,
      MockHttpServletRequestBuilder request,
      String requestId) {
    return request
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenFor("network-service"))
        .header(Correlation.REQUEST_ID_HEADER, requestId)
        .header(IdentityHeaders.USER_ID, userId.toString())
        .header(IdentityHeaders.USER_ROLES, role)
        .header(IdentityHeaders.EXTERNAL_REFERENCE, reference);
  }
}
