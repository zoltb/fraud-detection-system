package hu.zoltanb.projects.fraud.service.detector;

import hu.zoltanb.projects.fraud.model.Transaction;

import java.util.List;

public interface FraudDetector {
    void check(Transaction tx, List<String> fraudTypes);
}
