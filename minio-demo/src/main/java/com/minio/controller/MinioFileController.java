package com.minio.controller;


import com.minio.service.MinioFileService;
import com.minio.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import java.util.*;



@RestController
@RequestMapping(value = "/file")
@Slf4j
@CrossOrigin // 允许跨域
public class MinioFileController {


    @Autowired
    private MinioFileService minioFileService;

    @RequestMapping(value = "/home/upload")
    public ModelAndView homeUpload() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("upload");
        return modelAndView;
    }

    /**
     * 根据文件大小和文件的md5校验文件是否存在
     * 暂时使用Redis实现，后续需要存入数据库
     * 实现秒传接口
     *
     * @param md5 文件的md5
     * @return 操作是否成功
     */
    @GetMapping(value = "/check")
    public Map<String, Object> checkFileExists(String md5) {
        return minioFileService.checkFileExists(md5);
    }


    /**
     * 文件上传，适合大文件，集成了分片上传
     */
    @PostMapping(value = "/upload")
    public Map<String, Object> upload(HttpServletRequest req) {
        return minioFileService.upload(req);

    }

    /**
     * 文件合并
     *
     * @param shardCount 分片总数
     * @param fileName   文件名
     * @param md5        文件的md5
     * @param fileType   文件类型
     * @param fileSize   文件大小
     * @return 分片合并的状态
     */
    @GetMapping(value = "/merge")
    public Map<String, Object> merge(Integer shardCount, String fileName, String md5, String fileType,
                                     Long fileSize) {
        return minioFileService.merge(shardCount, fileName, md5, fileType, fileSize);
    }

}

