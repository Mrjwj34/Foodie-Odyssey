package org.jwj.fo.utils;

public interface Ilock {
    boolean tryLock(long timeoutSec);
    void unlock();
}
