package com.cinema.common.logging.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ResourceLockedException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.exception.exception.ValidationException;

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

        } catch (ValidationException
                | NotFoundException
                | ConflictException
                | UnauthorizedException
                | ForbiddenException
                | ResourceLockedException exception) {

            LOGGER.warn(
                    "Business exception in {}: {}",
                    joinPoint.getSignature(),
                    exception.getMessage());

            throw exception;

        } catch (Throwable exception) {

            LOGGER.error(
                    "Unexpected exception in {}",
                    joinPoint.getSignature(),
                    exception);

            throw exception;
        }

    }

}
