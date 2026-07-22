package com.cinema.common.lock.service;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class RedissonDistributedLockServiceTest {

    @Test
    void shouldAcquireLockSuccessfully()
            throws Exception {

        RedissonClient client = mock(RedissonClient.class);

        RLock lock = mock(RLock.class);

        when(
                client.getLock("test-lock"))
                .thenReturn(lock);

        when(
                lock.tryLock(
                        5,
                        30,
                        TimeUnit.SECONDS))
                .thenReturn(true);

        RedissonDistributedLockService service = new RedissonDistributedLockService(
                client);

        boolean result = service.tryLock(
                "test-lock",
                5,
                30,
                TimeUnit.SECONDS);

        assertThat(result)
                .isTrue();

        verify(lock)
                .tryLock(
                        5,
                        30,
                        TimeUnit.SECONDS);
    }

    @Test
    void shouldReturnFalseWhenCannotAcquireLock()
            throws Exception {

        RedissonClient client = mock(RedissonClient.class);

        RLock lock = mock(RLock.class);

        when(
                client.getLock("test-lock"))
                .thenReturn(lock);

        when(
                lock.tryLock(
                        anyLong(),
                        anyLong(),
                        any()))
                .thenReturn(false);

        RedissonDistributedLockService service = new RedissonDistributedLockService(
                client);

        boolean result = service.tryLock(
                "test-lock",
                5,
                30,
                TimeUnit.SECONDS);

        assertThat(result)
                .isFalse();

    }

    @Test
    void shouldUnlockWhenCurrentThreadOwnsLock() {

        RedissonClient client = mock(RedissonClient.class);

        RLock lock = mock(RLock.class);

        when(
                client.getLock("test-lock"))
                .thenReturn(lock);

        when(
                lock.isHeldByCurrentThread())
                .thenReturn(true);

        RedissonDistributedLockService service = new RedissonDistributedLockService(
                client);

        service.unlock(
                "test-lock");

        verify(lock)
                .unlock();

    }

}
