package co.edu.icesi.student360.network.domain.model;

/**
 * Who authored a {@code SUPPORTS} edge: the student rating their own incoming support, or a member
 * of the student-support team noting it on the student's behalf.
 */
public enum RaterType {
  SELF,
  SUPPORT_TEAM
}
