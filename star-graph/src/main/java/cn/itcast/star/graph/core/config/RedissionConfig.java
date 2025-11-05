package cn.itcast.star.graph.core.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis配置 - 配置RedissonClient（分布式锁、信号量）和StringRedisTemplate
 */
@Component
public class RedissionConfig {
    /**
     * Redisson客户端：提供分布式锁、信号量、有序集合等功能
     */
    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        config.useSingleServer().setAddress(String.format("redis://%s:%s", redisProperties.getHost(), redisProperties.getPort()));
        return Redisson.create(config);
    }

    /**
     * StringRedisTemplate：用于基础键值对操作
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisProperties redisProperties){
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
        lettuceConnectionFactory.start();
        return new StringRedisTemplate(lettuceConnectionFactory);
    }
}
