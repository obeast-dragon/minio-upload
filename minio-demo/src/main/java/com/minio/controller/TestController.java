package com.minio.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/28 15:22
 * @Version 1.0
 */

@RestController
@CrossOrigin
@Slf4j
public class TestController {

    @PostMapping("/uploadTest")
    public void upload(HttpServletRequest httpServletRequest){
        MultipartHttpServletRequest request = (MultipartHttpServletRequest) httpServletRequest;
        try{
            List<MultipartFile> files = request.getFiles("file");
            for (MultipartFile multipartFile : files) {
                System.out.println(multipartFile.getOriginalFilename());
            }
        }catch (Exception e){
            log.error("exceeds the configured maximum");
        }
//        MultipartFile file = request.getFile("file");
//        assert file != null;
//        System.out.println(file.getOriginalFilename());
    }
}
