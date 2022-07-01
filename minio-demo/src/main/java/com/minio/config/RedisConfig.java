//package com.minio.config;
//
//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.io.Serializable;
//
//
//@Configuration
//public class RedisConfig {
//
//    @Value("${spring.redis.host}")
//    private String redisHost;
//
//    @Value("${spring.redis.port}")
//    private String redisPort;
//
//    /**
//     * 通过配置RedisStandaloneConfiguration实例来
//     * 创建Redis Standolone模式的客户端连接创建工厂
//     * 配置hostname和port
//     *
//     * @return LettuceConnectionFactory
//     */
//    @Bean
//    public JedisConnectionFactory redisConnectionFactory() {
//        return new JedisConnectionFactory(new RedisStandaloneConfiguration(redisHost, Integer.parseInt(redisPort)));
//    }
//
//    /**
//     * 保证序列化之后不会乱码的配置
//     *
//     * @param connectionFactory connectionFactory
//     * @return RedisTemplate
//     */
//    @Bean(name = "jsonRedisTemplate")
//    public RedisTemplate<String, Serializable> redisTemplate(JedisConnectionFactory connectionFactory) {
//        return getRedisTemplate(connectionFactory, genericJackson2JsonRedisSerializer());
//    }
//
//    /**
//     * 解决：
//     * org.springframework.data.redis.serializer.SerializationException:
//     * Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported
//     *
//     * @return GenericJackson2JsonRedisSerializer
//     */
//    @Bean
//    @Primary // 当存在多个Bean时，此bean优先级最高
//    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        // 解决查询缓存转换异常的问题
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.WRAPPER_ARRAY);
//        // 支持 jdk 1.8 日期   ---- start ---
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        objectMapper.registerModule(new Jdk8Module())
//                .registerModule(new JavaTimeModule())
//                .registerModule(new ParameterNamesModule());
//        // --end --
//        return new GenericJackson2JsonRedisSerializer(objectMapper);
//    }
//
//    /**
//     * 注入redis分布式锁实现方案redisson
//     *
//     * @return RedissonClient
//     */
//    @Bean
//    public RedissonClient redisson() {
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort).setDatabase(0);
//        return Redisson.create(config);
//    }
//
//    /**
//     * 采用jdk序列化的方式
//     *
//     * @param connectionFactory connectionFactory
//     * @return RedisTemplate
//     */
//    @Bean(name = "jdkRedisTemplate")
//    public RedisTemplate<String, Serializable> redisTemplateByJdkSerialization(JedisConnectionFactory connectionFactory) {
//        return getRedisTemplate(connectionFactory, new JdkSerializationRedisSerializer());
//    }
//
//    private RedisTemplate<String, Serializable> getRedisTemplate(JedisConnectionFactory connectionFactory,
//                                                                 RedisSerializer<?> redisSerializer) {
//        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(redisSerializer);
//
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashValueSerializer(redisSerializer);
//        connectionFactory.afterPropertiesSet();
//        redisTemplate.setConnectionFactory(connectionFactory);
//        return redisTemplate;
//    }
//}
//
