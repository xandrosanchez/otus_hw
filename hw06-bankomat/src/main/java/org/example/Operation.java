package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Operation {
    private static final Logger logger = LoggerFactory.getLogger(Operation.class);

    private final Map<BanknoteDenomination, Long> banknotes = new HashMap<>();

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    private Long amount = 0L;

    public Map<BanknoteDenomination, Long> getBanknotes() {
        return banknotes;
    }

    public void setBanknotes(BanknoteDenomination denomination, long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Количество купюр не может быть отрицательным");
        }
        banknotes.put(denomination, count);
        amount += denomination.getValue() * count;
    }

    public void setBanknotes(Map<BanknoteDenomination, Long> map) {
        banknotes.putAll(map);
    }

    public void receivingLog() {
        logger.info("Количество купюр выдано:");
        for (Map.Entry<BanknoteDenomination, Long> entry : banknotes.entrySet()) {
            if (entry.getValue() > 0) {
                logger.info("{} рублей: {}", entry.getKey().getValue(), entry.getValue());
            }
        }
        logger.info("Общая сумма: {}", amount);
    }

    public void acceptanceLog() {
        logger.info("Количество купюр внесено:");
        for (Map.Entry<BanknoteDenomination, Long> entry : banknotes.entrySet()) {
            if (entry.getValue() > 0) {
                logger.info("{} рублей: {}", entry.getKey().getValue(), entry.getValue());
            }
        }
        logger.info("Общая сумма: {}", amount);
    }
}
