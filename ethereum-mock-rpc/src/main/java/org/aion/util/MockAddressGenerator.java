package org.aion.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MockAddressGenerator {

    private static final Random random = new Random(42L);
    private static final Lock lock = new ReentrantLock();

    // TODO: this can be prettier
    public static byte[] getEthereumAddress() {
        lock.lock();
        try {
            byte[] b = new byte[20];
            random.nextBytes(b);
            return b;
        } finally {
            lock.unlock();
        }
    }

    public static byte[] getAionAddress() {
        lock.lock();
        try {
            byte[] b = new byte[32];
            random.nextBytes(b);
            return b;
        } finally {
            lock.unlock();
        }
    }
}
