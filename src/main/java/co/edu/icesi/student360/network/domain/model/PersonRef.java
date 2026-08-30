package co.edu.icesi.student360.network.domain.model;

/**
 * A person to connect to a student: either an existing one, identified by {@code reference} (a
 * cross-service id like {@code A-2001}, or a network-generated {@code P-<uuid>}), or a brand new
 * one with no {@code reference} yet — the handler generates one before the first write so the
 * caller always gets an id back.
 */
public record PersonRef(String reference, PersonKind kind, String displayName) {}
