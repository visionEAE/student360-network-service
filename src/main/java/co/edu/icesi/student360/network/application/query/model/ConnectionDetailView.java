package co.edu.icesi.student360.network.application.query.model;

import java.util.List;

/** One person of a student's support network, opened: who they are, how to reach them, ratings. */
public record ConnectionDetailView(
    String studentId, PersonView person, ContactView contact, List<EdgeView> edges) {}
