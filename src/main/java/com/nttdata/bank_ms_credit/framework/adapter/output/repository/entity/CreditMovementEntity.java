package com.nttdata.bank_ms_credit.framework.adapter.output.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("credit_movements")
public class CreditMovementEntity {

    @Id
    private Long id;
    @Column("operation_code")
    private String operationCode;
    @Column("idempotency_key")
    private String idempotencyKey;
    @Column("credit_id")
    private Long creditId;
    @Column("movement_type")
    private String movementType;
    private BigDecimal amount;
    @Column("debt_after")
    private BigDecimal debtAfter;
    private String currency;
    @Column("payment_source")
    private String paymentSource;
    @Column("occurred_at")
    private OffsetDateTime occurredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getCreditId() { return creditId; }
    public void setCreditId(Long creditId) { this.creditId = creditId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getDebtAfter() { return debtAfter; }
    public void setDebtAfter(BigDecimal debtAfter) { this.debtAfter = debtAfter; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPaymentSource() { return paymentSource; }
    public void setPaymentSource(String paymentSource) { this.paymentSource = paymentSource; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}
