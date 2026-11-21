package hu.zoltanb.projects.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "transactions")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_id_seq")
    @SequenceGenerator(
            name = "transaction_id_seq",
            sequenceName = "transaction_entity_seq",
            allocationSize = 50 // number of IDs Hibernate reserves per sequence call
    )
    private Long id;

    private Long transactionId;
    private Long userId;
    private BigDecimal amount;
    private Long merchantId;
    private LocalDateTime createdAt;
    private boolean fraud;
    private List<String> fraudTypes;
    private Integer partition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionEntity that)) return false;
        return Objects.equals(getTransactionId(), that.getTransactionId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTransactionId());
    }
}
