# student360-network-service

Support network for Student 360° (port **8085**): a weighted, person-rated graph of who supports
a student, so both the student and the student-support team can see, at a glance, who their
primary support actually is — not just who is formally assigned to them.

## Two stores, on purpose

* **Neo4j** holds the graph: `Person` nodes (`STUDENT | ADVISOR | PROFESSOR | FAMILY | PEER |
  COUNSELOR | OTHER`) and `SUPPORTS` edges, directed from whoever provides support to the student
  receiving it, carrying a **1–10 weight**, a relationship label, and who rated it (`SELF` or
  `SUPPORT_TEAM`). Two raters may independently keep their own edge for the same pair — the
  student's own opinion of their advisor and the advisor's own note about the relationship
  coexist; neither silently overwrites the other.
* **Postgres** (`network` schema) holds only what every service already has for auditability: the
  outbox and the append-only audit trail. The graph itself never lives there. See
  `student360-infra/docs/network-contract.md` for the full contract this implements.

## Endpoints

| Method | Path | Handler |
|---|---|---|
| `POST` | `/api/network/students/{id}/connections` | `UpsertConnectionCommand` (new or reused person) |
| `PATCH` | `/api/network/students/{id}/connections/{personReference}` | `UpsertConnectionCommand` (identified person) |
| `DELETE` | `/api/network/students/{id}/connections/{personReference}` | `RemoveConnectionCommand` (only the caller's own edge) |
| `GET` | `/api/network/students/{id}/support-network` | `GetSupportNetworkQuery` |
| `GET` | `/api/network/advisors/me/students/{id}/support-network` | same query, advisor path |

A response's `connections` are ranked by the caller's own rating of each relationship (falling
back to whichever other rating is on file when the caller has not rated it yet); `primarySupport`
is the top of that ranking — literally "who is this student's main support, as far as they're
concerned" — and `averageWeight` is the mean of those same ranked weights.

## CQRS, same discipline as every other service

`application/command` for writes (audited `STATE_CHANGE`, an outbox event even though the record
itself lives in Neo4j), `application/query` for reads (audited `DATA_ACCESS`). Handlers depend
only on `domain/port.SupportNetworkRepository`; `infrastructure/persistence` is the only adapter,
built on `Neo4jClient` with explicit Cypher rather than Spring Data Neo4j's object-graph mapping —
a `SUPPORTS` edge is queried from either direction and two raters must keep independent edges for
the same pair, which is far more predictable to express directly than to coax out of an
object-relational-style mapper. The ranking rule itself (`SupportNetworkAssembler`) is plain Java,
Neo4j-free, and unit-tested on its own.

## Documented simplification (stage 1)

Unlike `support-service`, this service holds no assignment data of its own. Advisor access here
(`StudentNetworkAccessPolicy`) is authorized by **role** alone (any authenticated `ADVISOR` may
read or write a `SUPPORT_TEAM`-tagged connection for any student), not by an active assignment to
that specific student — the contract's ideal is assignment-based, matching `support-service`'s own
rule. Every write is still audited under the real advisor's identity, so the trail stays honest;
tightening the check to a real assignment means giving this service a way to ask `support-service`
"is this advisor assigned to this student", which does not yet exist as a service-to-service
endpoint and is out of scope here.

## Run · Verify

```bash
cd ../student360-infra && make up && make build-common && make run-network-service
mvn verify   # pure ranking-rule unit tests + Testcontainers (Postgres + Neo4j) flow tests
```

`SupportNetworkFlowIntegrationTest` is the phase gate: ranking by the caller's own weight with a
correct `primarySupport`; the student's and the support team's edges for the same person coexist;
`PATCH`/`DELETE` touch only the caller's own edge; a student cannot write another student's
network (`403`, audited `DENIED`); an out-of-range weight is `400`; no service token is `401`.
