package com.cinema.common.lock.aspect;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cinema.common.exception.code.CommonErrorCode;
import com.cinema.common.exception.exception.ResourceLockedException;
import com.cinema.common.lock.annotation.DistributedLock;
import com.cinema.common.lock.service.DistributedLockService;
import com.cinema.common.lock.util.LockKeyGenerator;

@Aspect
@Component
public class DistributedLockAspect {

    private final DistributedLockService lockService;

    public DistributedLockAspect(
            DistributedLockService lockService) {

        this.lockService = lockService;
    }

    @Around("@annotation(lock)")
    public Object execute(
            ProceedingJoinPoint joinPoint,
            DistributedLock lock) throws Throwable {

        String key = LockKeyGenerator.generate(
                lock.key());

        boolean acquired = lockService.tryLock(
                key,
                lock.waitTime(),
                lock.leaseTime(),
                TimeUnit.SECONDS);

        if (!acquired) {
            throw new ResourceLockedException(CommonErrorCode.RESOURCE_LOCKED);
        }

        try {

            return joinPoint.proceed();

        } finally {

            lockService.unlock(key);

        }

    }

}
