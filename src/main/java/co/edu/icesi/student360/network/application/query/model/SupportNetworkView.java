package co.edu.icesi.student360.network.application.query.model;

import java.util.List;

public record SupportNetworkView(
    String studentId,
    List<ConnectionView> connections,
    ConnectionView primarySupport,
    Double averageWeight) {}
