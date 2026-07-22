package com.cinema.common.lock.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.cinema.common.exception.exception.ResourceLockedException;
import com.cinema.common.lock.annotation.DistributedLock;
import com.cinema.common.lock.service.DistributedLockService;

class DistributedLockAspectTest {

    interface TestService {

        String execute();

    }

    static class TestServiceImpl
            implements TestService {

        @Override
        @DistributedLock(key = "test")
        public String execute() {

            return "success";
        }
    }

    @Test
    void shouldExecuteMethodWhenLockAcquired() {

        DistributedLockService service = mock(DistributedLockService.class);

        when(
                service.tryLock(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any()))
                .thenReturn(true);

        DistributedLockAspect aspect = new DistributedLockAspect(service);

        AspectJProxyFactory factory = new AspectJProxyFactory(
                new TestServiceImpl());

        factory.addAspect(aspect);

        TestService proxy = factory.getProxy();

        String result = proxy.execute();

        assertThat(result)
                .isEqualTo("success");

        verify(service)
                .unlock("lock:test");

    }

    @Test
    void shouldThrowExceptionWhenLockFailed() {

        DistributedLockService service = mock(
                DistributedLockService.class);

        when(
                service.tryLock(
                        anyString(),
                        anyLong(),
                        anyLong(),
                        any()))
                .thenReturn(false);

        DistributedLockAspect aspect = new DistributedLockAspect(
                service);

        AspectJProxyFactory factory = new AspectJProxyFactory(
                new TestServiceImpl());

        factory.addAspect(aspect);

        TestService proxy = factory.getProxy();

        assertThatThrownBy(
                proxy::execute)
                .isInstanceOf(
                        ResourceLockedException.class);

    }

}
