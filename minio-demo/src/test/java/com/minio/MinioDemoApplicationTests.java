package com.minio;



import com.minio.config.MinioConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class MinioDemoApplicationTests {




    @Autowired
    private MinioConfig minioConfig;
    @Test
    void contextLoads() throws Exception {
        System.out.println(minioConfig.getAccessKey());
        System.out.println(minioConfig.getEndpoint());
    }

}
