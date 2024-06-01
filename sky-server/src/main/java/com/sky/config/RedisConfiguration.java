package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 创建RedisTemplate对象，来操作Redis
 */
@Configuration
@Slf4j
public class RedisConfiguration {

    //Bean的作用在于这个创建方法需要一个RedisConnectionFactory对象，这个对象由Spring已经创建好
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板对象");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置redis key的序列号器（这是自己创建RedisTemplate的目的）,也方便在可视化界面看到正确的key而不是乱码
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    };
}
