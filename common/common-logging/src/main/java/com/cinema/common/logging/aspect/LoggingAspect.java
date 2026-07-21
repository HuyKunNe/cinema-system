package com.cinema.common.logging.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.cinema.common.logging.annotation.LogExecutionTime;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Slf4j
public class LoggingAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(
            ProceedingJoinPoint joinPoint,
            LogExecutionTime logExecutionTime)
            throws Throwable {

        long start = System.currentTimeMillis();

        try {

            return joinPoint.proceed();

        } finally {

            long executionTime = System.currentTimeMillis() - start;

            log.info(
                    "{} executed in {} ms",
                    joinPoint.getSignature().toShortString(),
                    executionTime);

        }

    }

}
