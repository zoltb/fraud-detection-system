package hu.zoltanb.projects.fraud.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
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
}