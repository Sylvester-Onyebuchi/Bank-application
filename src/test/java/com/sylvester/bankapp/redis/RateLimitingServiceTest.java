package com.sylvester.bankapp.redis;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(proxyManager);
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
    }

    @Test
    void resolveBucketBuildsLoginBucketWithLoginLimits() {
        Bucket bucket = resolveBucket(RateLimitType.LOGIN);

        assertThat(bucket).isEqualTo(bucketProxy);
        assertLimit(5, Duration.ofMinutes(2));
    }

    @Test
    void resolveBucketBuildsTransferBucketWithTransferLimits() {
        resolveBucket(RateLimitType.TRANSFER);

        assertLimit(10, Duration.ofMinutes(2));
    }

    @Test
    void resolveBucketBuildsGeneralBucketWithDefaultLimits() {
        resolveBucket(RateLimitType.GENERAL);

        assertLimit(20, Duration.ofMinutes(1));
    }

    private Bucket resolveBucket(RateLimitType type) {
        when(remoteBucketBuilder.build(eq("rate-key"), org.mockito.ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
                .thenReturn(bucketProxy);

        return rateLimitingService.resolveBucket2("rate-key", type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertLimit(long capacity, Duration refillInterval) {
        ArgumentCaptor<Supplier<BucketConfiguration>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(remoteBucketBuilder).build(eq("rate-key"), captor.capture());

        Bandwidth limit = captor.getValue().get().getBandwidths()[0];
        assertThat(limit.getCapacity()).isEqualTo(capacity);
        assertThat(limit.getRefillTokens()).isEqualTo(capacity);
        assertThat(limit.getRefillPeriodNanos()).isEqualTo(refillInterval.toNanos());
        assertThat(limit.isRefillIntervally()).isTrue();
    }
}
