package com.minio.controller;


import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import com.minio.entity.R;
import com.minio.entity.ResponseEntry;
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

    @Autowired
    private ResponseEntry res;

    @PostMapping(value = "/upload")
    public R upload(HttpServletRequest httpServletRequest) {
        MultipartHttpServletRequest request = (MultipartHttpServletRequest) httpServletRequest;
        MultipartFile multipartFile = request.getFile("file");
        if (multipartFile != null) {
            res.put("stopStatus", true);
            res.put("fileName", multipartFile.getOriginalFilename());
            res.put("fileSize", multipartFile.getSize());
            String filenameExtension = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
            minioService.uploadShard(multipartFile, res, filenameExtension);
        } else {
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

    @RequestMapping(value = "/stopStatus")
    public void setStopStatus(HttpServletRequest request) {
        /**
         * 1 -> true 上传
         * 0 -> false 暂停
         * */
        String strStopStatus = request.getParameter("stopStatus");
        boolean stopStatus = strStopStatus.equals("1");
        res.put("stopStatus", stopStatus);
        log.info("上传暂停 {}", stopStatus);
    }


    /**
     * 获取实现上传进度
     */
    @GetMapping(value = "/percent", produces = "application/json;charset=UTF-8")
    public R getUploadPercent() {
        String uploadPercent = res.get("uploadPercent") == null ? "0" : String.valueOf(res.get("uploadPercent"));
        String uploadSize = res.get("uploadSize") == null ? "0" : String.valueOf(res.get("uploadSize"));
        String fileSize = res.get("fileSize") == null ? "0" : String.valueOf(res.get("fileSize"));
        JSONObject data = new JSONObject();
        try {
            data.append("uploadPercent", uploadPercent);
            data.append("uploadSize", uploadSize);
            data.append("fileSize", fileSize);
            data.append("fileName", res.get("fileName"));
        } catch (JSONException e) {
            log.error("上传进度查询失败{}", e.getMessage());
        }
        return R.ok().put("obj", data);
    }


    /**
     * 重置上传进度
     **/
    @RequestMapping("/resetPercent")
    @ResponseBody
    public String resetPercent() {
        res.put("uploadPercent", 0);
        return "重置进度";
    }


}
