package com.nttdata.bank_ms_credit.framework.adapter.output.repository.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Table("credits")
public class CreditEntity {

    @Id
    private Long id;
    @Column("credit_number")
    private String creditNumber;
    @Column("customer_number")
    private Long customerNumber;
    @Column("credit_type")
    private String creditType;
    private String status;
    @Column("original_amount")
    private BigDecimal originalAmount;
    @Column("outstanding_debt")
    private BigDecimal outstandingDebt;
    private String currency;
    @Column("created_at")
    private OffsetDateTime createdAt;
    @Column("updated_at")
    private OffsetDateTime updatedAt;
    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCreditNumber() { return creditNumber; }
    public void setCreditNumber(String creditNumber) { this.creditNumber = creditNumber; }
    public Long getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(Long customerNumber) { this.customerNumber = customerNumber; }
    public String getCreditType() { return creditType; }
    public void setCreditType(String creditType) { this.creditType = creditType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getOutstandingDebt() { return outstandingDebt; }
    public void setOutstandingDebt(BigDecimal outstandingDebt) { this.outstandingDebt = outstandingDebt; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
