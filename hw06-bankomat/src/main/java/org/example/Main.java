package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Bankomat b = new Bankomat();
        b.receiving(13890L);
        b.receivingAll();
        b.acceptance(getOperationByTest());
    }

    private static Operation getOperationByTest() {
        Operation op = new Operation();
        Map<BanknoteDenomination, Long> banknotes = new HashMap<>();
        Random random = new Random();
        for (BanknoteDenomination denomination : BanknoteDenomination.values()) {
            banknotes.put(denomination, (long)(random.nextInt(91) + 10));
        }
        op.setBanknotes(banknotes);
        Long amount = 0L;
        for (Map.Entry<BanknoteDenomination, Long> entry : banknotes.entrySet()) {
            if (entry.getValue() != 0) {
                amount += entry.getValue() * entry.getKey().getValue();
            }
        }
        op.setAmount(amount);
        return op;
    }
}