package com.cinema.common.lock.service;

import java.util.concurrent.TimeUnit;

public interface DistributedLockService {

    boolean tryLock(
            String key,
            long waitTime,
            long leaseTime,
            TimeUnit unit);

    void unlock(
            String key);

}
