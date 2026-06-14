package hu.zoltanb.projects.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
@Data
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
            allocationSize = 50 // Megmondja a Hibernate-nek, hogy egyszerre 50 ID-t foglaljon le a memóriában
    )
    private Long id;

    private Long transactionId;
    private Long userId;
    private BigDecimal amount;
    private Long merchantId;
    private LocalDateTime createdAt;

    private boolean fraud;
    //private String fraudType;
    List<String> fraudTypes;
    private Integer partition;
}
