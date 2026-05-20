package hu.zoltanb.projects.fraud.model;

import java.util.List;

public record FraudCheckResult(boolean isFraud, List<String> fraudTypes) {
}
