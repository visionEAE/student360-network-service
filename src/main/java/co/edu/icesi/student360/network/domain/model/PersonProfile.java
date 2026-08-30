package co.edu.icesi.student360.network.domain.model;

/** A person node as the graph holds it: who they are, plus whatever contact detail was stored. */
public record PersonProfile(
    String reference, PersonKind kind, String displayName, PersonContact contact) {}
