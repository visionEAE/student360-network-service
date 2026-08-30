package co.edu.icesi.student360.network.domain.service;

import co.edu.icesi.student360.common.api.exception.AccessDeniedForSubjectException;
import co.edu.icesi.student360.common.audit.AuthorizationBasis;
import co.edu.icesi.student360.common.audit.AuthorizationBasisHolder;
import co.edu.icesi.student360.common.identity.Identity;
import co.edu.icesi.student360.common.identity.IdentityContext;
import co.edu.icesi.student360.network.domain.model.RaterType;

/**
 * Who may read or write a student's support network: the student themself (their own incoming
 * support), an advisor, or an admin.
 *
 * <p><strong>Documented simplification (stage 1):</strong> unlike {@code support-service}, this
 * service holds no assignment data of its own, so an advisor's access here is authorized by
 * <em>role</em> alone ({@code STAFF_ROLE}), not by an active assignment to the specific student —
 * every write an advisor makes is still audited under their own identity, so the trail stays
 * honest, but the check is coarser than the contract's ideal. Tightening this to a real assignment
 * check means giving network-service a way to ask {@code support-service} "is this advisor assigned
 * to this student" — deliberately left out of this change; support-service does not yet expose that
 * check as a service-to-service endpoint.
 */
public final class StudentNetworkAccessPolicy {

  static final String STUDENT = "STUDENT";
  static final String ADVISOR = "ADVISOR";
  static final String ADMIN = "ADMIN";
  static final String SUBJECT_TYPE = "STUDENT";

  private StudentNetworkAccessPolicy() {}

  /** Read access: same relationships as a write, expressed once so both paths agree. */
  public static void assertCanRead(String studentReference) {
    resolveRater(studentReference);
  }

  /** Write access, resolving in the same step who is authoring the edge and under what name. */
  public static Rater assertCanWrite(String studentReference) {
    return resolveRater(studentReference);
  }

  private static Rater resolveRater(String studentReference) {
    Identity caller = IdentityContext.require();
    if (caller.hasRole(ADMIN)) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.ADMIN_ROLE);
      return new Rater(RaterType.SUPPORT_TEAM, "ADMIN");
    }
    if (caller.hasRole(STUDENT) && studentReference.equals(caller.externalReference())) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.SELF);
      return new Rater(RaterType.SELF, studentReference);
    }
    if (caller.hasRole(ADVISOR) && caller.externalReference() != null) {
      AuthorizationBasisHolder.grant(AuthorizationBasis.STAFF_ROLE);
      return new Rater(RaterType.SUPPORT_TEAM, caller.externalReference());
    }
    throw new AccessDeniedForSubjectException(SUBJECT_TYPE, studentReference);
  }

  /** Who is acting, and under which name their edges should be recorded. */
  public record Rater(RaterType type, String reference) {}
}
