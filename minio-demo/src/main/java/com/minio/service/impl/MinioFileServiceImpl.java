package com.minio.service.impl;


import cn.hutool.core.io.FileUtil;
import com.minio.config.MinioConfig;
import com.minio.entity.MergeShardArgs;
import com.minio.entity.OssFile;
import com.minio.entity.R;
import com.minio.exception.StatusCode;
import com.minio.service.MinioFileService;
import com.minio.service.MinioService;
import com.minio.template.MinioTemplate;
import com.minio.util.FileTypeUtil;
import com.minio.util.Md5Util;

import com.minio.util.ShardFileUtil;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.io.*;

import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/29 17:50
 * @Version 1.0
 */
@Service
@Slf4j
public class MinioFileServiceImpl implements MinioFileService {

    @Resource
    MinioTemplate minioTemplate;

    @Resource
    private MinioConfig minioConfig;

//    @Resource(name = "jsonRedisTemplate")
//    private RedisTemplate<String, Serializable> jsonRedisTemplate;

    private static final String MD5_KEY = "cn:lyf:minio:demo:file:md5List";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public Map<String, Object> upload(HttpServletRequest req) {
        Map<String, Object> map = new HashMap<>();

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) req;

        // 获得文件分片数据
        MultipartFile file = multipartRequest.getFile("data");

        // 上传过程中出现异常，状态码设置为50000
        if (file == null) {
            map.put("status", StatusCode.FAILURE.getCode());
            return map;
        }
        // 分片第几片
        int index = Integer.parseInt(multipartRequest.getParameter("index"));
        // 总片数
        int total = Integer.parseInt(multipartRequest.getParameter("total"));
        // 获取文件名
        String fileName = multipartRequest.getParameter("name");

        String md5BucketName = multipartRequest.getParameter("md5");

        // 创建文件桶
        minioTemplate.makeBucket(md5BucketName);
        String curName = String.valueOf(index);

        log.info("index: {}, total:{}, fileName:{}, md5:{}, objectName:{}", index, total, fileName, md5BucketName, curName);

        // 当不是最后一片时，上传返回的状态码为20001
        if (index < total) {
            try {
                // 上传文件
                OssFile ossFile = minioTemplate.putChunkObject(file.getInputStream(), md5BucketName, curName);
                log.info("{} upload success {}", curName, ossFile);

                // 设置上传分片的状态
                map.put("status", StatusCode.ALONE_CHUNK_UPLOAD_SUCCESS.getCode());
                return map;
            } catch (Exception e) {
                e.printStackTrace();
                map.put("status", StatusCode.FAILURE.getCode());
                return map;
            }
        } else {
            // 为最后一片时状态码为20002
            try {
                // 上传文件
                minioTemplate.putChunkObject(file.getInputStream(), md5BucketName, curName);

                // 设置上传分片的状态
                map.put("status", StatusCode.ALL_CHUNK_UPLOAD_SUCCESS.getCode());
                return map;
            } catch (Exception e) {
                e.printStackTrace();
                map.put("status", StatusCode.FAILURE.getCode());
                return map;
            }
        }
    }

    @Override
    public Map<String, Object> merge(Integer shardCount, String fileName, String md5, String fileType, Long fileSize) {
        Map<String, Object> retMap = new HashMap<>();

        try {
            // 查询片数据
            List<String> objectNameList = minioTemplate.listObjectNames(md5);
            if (shardCount != objectNameList.size()) {
                // 失败
                retMap.put("status", StatusCode.FAILURE.getCode());
            } else {
                // 开始合并请求
                String targetBucketName = minioConfig.getBucketName();
                String filenameExtension = StringUtils.getFilenameExtension(fileName);
                String fileNameWithoutExtension = UUID.randomUUID().toString();
                String objectName = fileNameWithoutExtension + "." + filenameExtension;
                minioTemplate.composeObject(md5, targetBucketName, objectName);

                log.info("桶：{} 中的分片文件，已经在桶：{},文件 {} 合并成功", md5, targetBucketName, objectName);

                // 合并成功之后删除对应的临时桶
                minioTemplate.removeBucket(md5, true);
                log.info("删除桶 {} 成功", md5);

                // 计算文件的md5
                String fileMd5 = null;
                try (InputStream inputStream = minioTemplate.getObject(targetBucketName, objectName)) {
                    fileMd5 = Md5Util.calculateMd5(inputStream);
                } catch (IOException e) {
                    log.error("", e);
                }

                // 计算文件真实的类型
                String type = null;
                List<String> typeList = new ArrayList<>();
                try (InputStream inputStreamCopy = minioTemplate.getObject(targetBucketName, objectName)) {
                    typeList.addAll(FileTypeUtil.getFileRealTypeList(inputStreamCopy, fileName, fileSize));
                } catch (IOException e) {
                    log.error("", e);
                }

                // 并和前台的md5进行对比
                if (!ObjectUtils.isEmpty(fileMd5) && !ObjectUtils.isEmpty(typeList) && fileMd5.equalsIgnoreCase(md5) && typeList.contains(fileType.toLowerCase(Locale.ENGLISH))) {
                    // 表示是同一个文件, 且文件后缀名没有被修改过
                    String url = minioTemplate.getPresignedObjectUrl(targetBucketName, objectName);

                    // 存入redis中
//                    jsonRedisTemplate.boundHashOps(MD5_KEY).put(fileMd5, url);

                    // 成功
                    retMap.put("status", StatusCode.SUCCESS.getCode());
                } else {
                    log.info("非法的文件信息: 分片数量:{}, 文件名称:{}, 文件fileMd5:{}, 文件真实类型:{}, 文件大小:{}",
                            shardCount, fileName, fileMd5, typeList, fileSize);
                    log.info("非法的文件信息: 分片数量:{}, 文件名称:{}, 文件md5:{}, 文件类型:{}, 文件大小:{}",
                            shardCount, fileName, md5, fileType, fileSize);

                    // 并需要删除对象
                    minioTemplate.deleteObject(targetBucketName, objectName);
                    retMap.put("status", StatusCode.FAILURE.getCode());
                }
            }
        } catch (Exception e) {
            log.error("", e);
            // 失败
            retMap.put("status", StatusCode.FAILURE.getCode());
        }
        return retMap;
    }


    @Override
    public R checkFileExists(String md5) {
        R r = new R();
//        if (ObjectUtils.isEmpty(md5)) {
//            return r.put("code", StatusCode.NULL_ARGS.getCode());
//        }
//        // 查询
//        String url = (String) jsonRedisTemplate.boundHashOps(MD5_KEY).get(md5);
//
//        // 文件不存在
//        if (ObjectUtils.isEmpty(url)) {
//            return r.put("code", StatusCode.NOT_FOUND.getCode());
//        }
//        //文件存在
        return r.put("code", StatusCode.FOUND.getCode());
    }
}
