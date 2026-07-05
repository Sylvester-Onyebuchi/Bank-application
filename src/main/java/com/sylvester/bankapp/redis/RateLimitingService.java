package com.sylvester.bankapp.redis;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingService {



    private final ProxyManager<String> proxyManager;

    public Bucket resolveBucket2(String key, RateLimitType type) {
        Supplier<BucketConfiguration> configSupplier =
                () -> configurationFor(type);

        return proxyManager.builder().build(key, configSupplier);
    }

    private BucketConfiguration configurationFor(RateLimitType type) {

        Bandwidth limit;

        switch (type) {

            case LOGIN -> limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5,Duration.ofMinutes(2))
                    .build();

            case TRANSFER -> limit = Bandwidth.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofMinutes(2))
                    .build();

            default -> limit = Bandwidth.builder()
                    .capacity(20)
                    .refillIntervally(20,Duration.ofMinutes(1))
                    .build();

        }

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
