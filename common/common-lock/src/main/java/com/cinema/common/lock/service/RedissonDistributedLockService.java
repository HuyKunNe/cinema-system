package com.cinema.common.lock.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedissonDistributedLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    public RedissonDistributedLockService(RedissonClient redissonClient) {

        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(
            String key,
            long waitTime,
            long leaseTime,
            TimeUnit unit) {

        try {

            RLock lock = redissonClient.getLock(key);

            return lock.tryLock(
                    waitTime,
                    leaseTime,
                    unit);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return false;
        }
    }

    @Override
    public void unlock(String key) {

        RLock lock = redissonClient.getLock(key);

        if (lock.isHeldByCurrentThread()) {

            lock.unlock();
        }

    }

}
