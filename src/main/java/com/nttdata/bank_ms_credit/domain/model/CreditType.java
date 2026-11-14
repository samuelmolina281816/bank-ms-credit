package com.nttdata.bank_ms_credit.domain.model;

public enum CreditType {
    PERSONAL("PER"),
    BUSINESS("BUS");

    private final String code;

    CreditType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
