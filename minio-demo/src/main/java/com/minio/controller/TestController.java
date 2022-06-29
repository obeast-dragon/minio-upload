package com.minio.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/28 15:22
 * @Version 1.0
 */

@RestController
@CrossOrigin
public class TestController {

    @PostMapping("/uploadTest")
    public void upload(HttpServletRequest httpServletRequest){
        MultipartHttpServletRequest request = (MultipartHttpServletRequest) httpServletRequest;
        MultipartFile file = request.getFile("file");
        assert file != null;
        System.out.println(file.getOriginalFilename());
    }
}
