package com.getvaas.distribution.engine.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);

    @Bean("customRedisConnectionFactory")
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled,
            @Value("${spring.data.redis.ssl.disable-peer-verification:false}") boolean sslDisablePeerVerification) {

        var redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);

        var clientConfigBuilder = LettuceClientConfiguration.builder()
                .commandTimeout(COMMAND_TIMEOUT);
        if (sslEnabled) {
            if (sslDisablePeerVerification) {
                clientConfigBuilder.useSsl().disablePeerVerification();
            } else {
                clientConfigBuilder.useSsl();
            }
        }

        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfigBuilder.build());
    }
}
