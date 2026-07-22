package com.cinema.common.logging.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.cinema..service..*(..))")
    public Object logExecution(
            ProceedingJoinPoint joinPoint)
            throws Throwable {

        long start = System.currentTimeMillis();

        try {

            Object result = joinPoint.proceed();

            LOGGER.info(
                    "{} executed in {} ms",
                    joinPoint.getSignature(),
                    System.currentTimeMillis() - start);

            return result;

        } catch (Throwable ex) {

            LOGGER.error(
                    "Exception in {}",
                    joinPoint.getSignature(),
                    ex);

            throw ex;

        }

    }

}
