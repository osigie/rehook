package com.osigie.rehook.configuration.rate_limiting;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class RateLimiterConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient(
            RedisProperties redisProperties
    ) {
        RedisURI uri = RedisURI.Builder.redis(
                redisProperties.getHost(),
                redisProperties.getPort()
        ).build();

        return RedisClient.create(uri);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        return redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
    }

    @Bean
    public ProxyManager<String> lettuceProxyManager(
            StatefulRedisConnection<String, byte[]> redisConnection
    ) {
        return LettuceBasedProxyManager
                .builderFor(redisConnection)
                .build();
    }


    public Supplier<BucketConfiguration> bucketConfiguration(long capacity, long refillTokens, Duration refillPeriod) {
        return () -> BucketConfiguration
                .builder()
                .addLimit(Bandwidth
                        .builder()
                        .capacity(capacity)
                        .refillGreedy(refillTokens, refillPeriod).build())
                .build();
    }
}
