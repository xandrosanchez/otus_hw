package org.example;

public enum BanknoteDenomination {
    FIVE_THOUSAND(5000),
    TWO_THOUSAND(2000),
    THOUSAND(1000),
    FIVE_HUNDRED(500),
    TWO_HUNDRED(200),
    ONE_HUNDRED(100),
    FIFTY(50),
    TEN(10);

    private final int value;

    BanknoteDenomination(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
