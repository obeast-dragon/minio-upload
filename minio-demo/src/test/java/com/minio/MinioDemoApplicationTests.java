package com.minio;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minio.dao.Md5FileNameDao;
import com.minio.entity.Md5FileNameEntry;
import com.minio.entity.ResponseEntry;
import com.minio.service.MinioService;
import com.minio.template.MinioTemplate;
import com.minio.util.Md5Util;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SpringBootTest
class MinioDemoApplicationTests {


    @Autowired
    private Md5FileNameDao md5FileNameDao;


    @Autowired
    private MinioTemplate minioTemplate;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ResponseEntry res;

    @Test
    void test() {
        List<String> strings = minioTemplate.listObjectNames("c3232cb67783d0d7b002701ed9c2a6d1");
        long shard = 51;
        List<String> list = containList(strings, shard);
        for (String s : list) {
            System.out.print(" " + s);
        }
    }

    private List<String> containList(List<String> objNames, long shardCount) {
        List<String> containList = new ArrayList<>();
        for (int i = 1; i <= shardCount; i++) {
            String str = String.valueOf(i);
            if (!objNames.contains(str)) {
                containList.add(str);
            }
        }
        return containList;
    }


    @Test
    void test2() {

    }

    @Test
    void contextLoads() throws Exception {
        String name = "C:\\Users\\WengX\\Desktop\\a.txt";
        String url = "https://175.178.183.32:9000/test/c6d5aae6-73dc-4660-a644-4aa2d3abbd98-3333.rar?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20220630%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20220630T022216Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=b78ccc15b4f910fe34979e86246ad355703d8ff37313bccedd042f4059320871";
        File file = new File(name);
        InputStream inputStream = new FileInputStream(file);
        String s = Md5Util.calculateMd5(inputStream);
        Md5FileNameEntry md5FileNameEntry = new Md5FileNameEntry(s, url);
//        md5FileNameDao.insert(md5FileNameEntry);

        Md5FileNameEntry md5FileNameEntry1 = md5FileNameDao.selectOne(new QueryWrapper<Md5FileNameEntry>()
                .eq("md5_file_name", s)
        );
        System.out.println(md5FileNameEntry1);
//        System.out.println(md5FileNameEntry1.getMd5FileName().equals(s));

    }

}
