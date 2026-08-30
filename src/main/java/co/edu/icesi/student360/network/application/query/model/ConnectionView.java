package co.edu.icesi.student360.network.application.query.model;

import java.util.List;

public record ConnectionView(PersonView person, List<EdgeView> edges) {}
