package co.edu.icesi.student360.network.domain.model;

/**
 * What core-service's directory knows about an institutional person: their published contact
 * details and two display lines ({@code headline} = program or department, {@code detail} = current
 * semester or the courses they teach). Never grades, balances or support-case data.
 */
public record DirectoryProfile(
    String reference, String displayName, String email, String headline, String detail) {}
