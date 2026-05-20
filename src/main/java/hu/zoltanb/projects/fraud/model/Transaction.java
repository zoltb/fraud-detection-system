package hu.zoltanb.projects.fraud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    //to fix JSON key
    @JsonProperty("transactionId")
    private Long transactionId;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("merchantId")
    private Long merchantId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "text[]")
    private List<String> fraudTypes;
}