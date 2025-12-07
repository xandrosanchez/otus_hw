package ru.otus;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class TwoThreadCounter {
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private int currentNumber = 1;
    private boolean increasing = true;
    private int turn = 1; // 1 - первый поток, 2 - второй поток

    public void firstThread() throws InterruptedException {
        while (true) {
            lock.lock();
            try {
                // Ждем, пока не наша очередь
                while (turn != 1) {
                    condition.await();
                }

                System.out.println("Поток 1: " + currentNumber);
                Thread.sleep(1000);
                turn = 2; // Передаем ход второму потоку
                condition.signal(); // Будим второй поток
            } finally {
                lock.unlock();
            }
        }
    }

    public void secondThread() throws InterruptedException {
        while (true) {
            lock.lock();
            try {
                // Ждем, пока не наша очередь
                while (turn != 2) {
                    condition.await();
                }

                System.out.println("Поток 2: " + currentNumber);
                Thread.sleep(1000);
                turn = 1; // Передаем ход первому потоку

                // После вывода второго потока меняем число
                if (increasing) {
                    if (currentNumber >= 10) {
                        increasing = false;
                        currentNumber--;
                    } else {
                        currentNumber++;
                    }
                } else {
                    if (currentNumber <= 1) {
                        increasing = true;
                        currentNumber++;
                    } else {
                        currentNumber--;
                    }
                }

                condition.signal(); // Будим первый поток
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        TwoThreadCounter counter = new TwoThreadCounter();

        Thread t1 = new Thread(() -> {
            try {
                counter.firstThread();
            } catch (InterruptedException e) {
                System.out.println("Поток 1 прерван");
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                counter.secondThread();
            } catch (InterruptedException e) {
                System.out.println("Поток 2 прерван");
            }
        });

        t1.start();
        t2.start();
    }
}