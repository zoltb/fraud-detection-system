package hu.zoltanb.projects.fraud.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name= "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private Long merchantId;
    private LocalDateTime createdAt;

    private boolean fraud;
    private String fraudType;
}
