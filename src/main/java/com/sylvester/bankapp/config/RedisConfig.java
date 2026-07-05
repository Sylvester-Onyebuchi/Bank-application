package com.sylvester.bankapp.config;



import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.ObjectMapper;

import java.net.SocketException;
import java.time.Duration;

@Configuration
public class RedisConfig {


    private final String password;
    private final String host;
    private final int port;

    public RedisConfig(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password}") String password
    ) {

        this.password = password;
        this.host = host;
        this.port = port;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
           RedisConnectionFactory redisConnectionFactory
    )   {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJacksonJsonRedisSerializer(new ObjectMapper()));
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJacksonJsonRedisSerializer(new ObjectMapper()));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisClient redisClient() {

        RedisURI uri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withPassword(password.toCharArray())
                .build();

        return RedisClient.create(uri);
    }


    @Bean
    public ProxyManager<String>  proxyManager(RedisClient redisClient) {
        var redisConnection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        var expirationStrategy = ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1));
        var clientConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(expirationStrategy);

        return LettuceBasedProxyManager.builderFor(redisConnection)
                .withClientSideConfig(clientConfig).build();
    }





}
