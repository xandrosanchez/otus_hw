package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Bankomat {
    private static final Logger logger = LoggerFactory.getLogger(Bankomat.class);
    private static Long amount = 0L;

    private static final Map<BanknoteDenomination, Long> banknotes = new HashMap<>();

    static {
        Random random = new Random();
        for (BanknoteDenomination denomination : BanknoteDenomination.values()) {
            banknotes.put(denomination, (long)(random.nextInt(91) + 10));
        }
        countingAmount();
    }

    public Bankomat() {
    }

    public void acceptance(Operation operation){
        banknotes.putAll(operation.getBanknotes());
        operation.acceptanceLog();
        countingAmount();
    }

    public void receiving(Long sum) {
        Operation operation = new Operation();
        logger.info("Попытка взять из банкомата {} рублей", sum);

        if (sum < amount){

            long remainingSum = sum;

            for (BanknoteDenomination denomination : BanknoteDenomination.values()) {
                if (banknotes.get(denomination) != 0) {
                    long count = remainingSum / denomination.getValue();
                    if (count != 0) {
                        remainingSum -= count * denomination.getValue();
                        operation.setBanknotes(denomination, count);
                    }
                }
            }

            if (remainingSum == 0) {
                logger.info("В банкомате хватит денег, чтобы выдать {} рублей", sum);
                amount -= sum;
                for (Map.Entry<BanknoteDenomination, Long> entry : operation.getBanknotes().entrySet()) {
                    BanknoteDenomination denomination = entry.getKey();
                    long count = entry.getValue();

                    banknotes.compute(denomination, (k, currentCount) -> currentCount - count);
                }
                operation.receivingLog();
            } else {
                logger.info("Не удалось выдать деньги");
            }
        }
    }

    public void receivingAll(){
        Operation operation = new Operation();
        logger.info("Попытка взять из банкомата {} рублей", amount);
        for (Map.Entry<BanknoteDenomination, Long> entry : banknotes.entrySet()) {
            BanknoteDenomination denomination = entry.getKey();
            long count = entry.getValue();
            operation.setBanknotes(denomination, count);
            entry.setValue(0L);
        }
        operation.receivingLog();
    }

    private static void countingAmount() {
        amount = 0L;
        for (Map.Entry<BanknoteDenomination, Long> entry : banknotes.entrySet()) {
            if (entry.getValue() != 0) {
                amount += entry.getValue() * entry.getKey().getValue();
            }
        }
        logger.info("В банкомате сейчас {} рублей", amount);
    }
}
