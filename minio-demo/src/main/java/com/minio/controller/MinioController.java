package com.minio.controller;


import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import com.minio.entity.R;
import com.minio.exception.StatusCode;
import com.minio.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "/shard")
@Slf4j
@CrossOrigin // 允许跨域
public class MinioController {

    @Autowired
    private MinioService minioService;

    @PostMapping(value = "/upload")
    public R upload(HttpServletRequest httpServletRequest){
        HttpSession session = httpServletRequest.getSession();
        MultipartHttpServletRequest request = (MultipartHttpServletRequest) httpServletRequest;
        MultipartFile multipartFile = request.getFile("file");
//        String fileType = request.getParameter("fileType");
        if (multipartFile != null){
            String filenameExtension = StringUtils.getFilenameExtension(multipartFile.getName());
            minioService.uploadShard(multipartFile, session, filenameExtension);

        }else {
            return R.error(StatusCode.FOUND.getCode(), StatusCode.FILE_IS_NULL.getMessage());
        }
        return R.ok();

    }

    @GetMapping(value = "/view")
    public ModelAndView index(HttpSession session) {
        session.getAttribute("uploadPercent");
        System.out.println(session.getId());
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index");
        return modelAndView;

    }

    /**
     * 获取实现上传进度
     */
    @GetMapping(value = "/percent", produces = "application/json;charset=UTF-8")
    public R getUploadPercent(HttpServletRequest request) {
        HttpSession session = request.getSession();
        int percent = session.getAttribute("uploadPercent") == null ? 0 : (int) session.getAttribute("uploadPercent");
        String uploadSize = session.getAttribute("uploadSize") == null ? null : (String) session.getAttribute("uploadSize");
        String fileSize = session.getAttribute("fileSize") == null ? null : (String) session.getAttribute("fileSize");
        log.info("上传进度{}已经上传大小{}", percent, uploadSize);
        JSONObject data = new JSONObject();
        try {
            data.append("fileName", session.getAttribute("fileName"));
            data.append("uploadPercent", percent);
            data.append("fileSize", fileSize);
            data.append("uploadSize", uploadSize);
        } catch (JSONException e) {
            log.error("上传进度查询失败{}", e.getMessage());
        }
        return R.ok().put("obj", data);
    }


}
