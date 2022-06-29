package com.minio.config;

import com.minio.template.MinioTemplate;
import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
@ConfigurationProperties(value = "minio")
public class MinioConfig {
    /**
     * 对象存储服务的URL
     */
    private String endpoint;

    /**
     * Access key就像用户ID，可以唯一标识你的账户。
     */
    private String accessKey;

    /**
     * Secret key是你账户的密码。
     */
    private String secretKey;

    /**
     * bucketName是你设置的桶的名称
     */
    private String bucketName;

    /**
     * 初始化一个MinIO客户端用来连接MinIO存储服务
     *
     * @return MinioClient
     */
    @Bean(name = "minioClient")
    public MinioClient initMinioClient() {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    /**
     * 初始化MinioTemplate，封装了一些MinIOClient的基本操作
     *
     * @return MinioTemplate
     */
    @Bean(name = "minioTemplate")
    public MinioTemplate minioTemplate() {
        return new MinioTemplate(initMinioClient(), this);
    }
}

