package com.nttdata.bank_ms_credit.domain.model;

public enum CreditMovementType {
    DISBURSEMENT("DIS"),
    PAYMENT("PAY");

    private final String code;

    CreditMovementType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
