package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.service.detector.FraudDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FraudDetectionService {

    private final List<FraudDetector> detectors;

    public FraudDetectionService(List<FraudDetector> detectors) {
        this.detectors = detectors;
    }
    public FraudCheckResult check(Transaction tx) {
        List<String> fraudTypes = new ArrayList<>();

        // Running detectors
        detectors.forEach(detector -> detector.check(tx, fraudTypes));

        return new FraudCheckResult(!fraudTypes.isEmpty(), fraudTypes);
    }
}