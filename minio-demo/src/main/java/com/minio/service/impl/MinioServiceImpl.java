package com.minio.service.impl;

import cn.hutool.core.io.FileUtil;
import com.minio.config.MinioConfig;
import com.minio.entity.MergeShardArgs;
import com.minio.entity.OssFile;
import com.minio.entity.R;
import com.minio.exception.StatusCode;
import com.minio.service.MinioService;
import com.minio.template.MinioTemplate;
import com.minio.util.FileTypeUtil;
import com.minio.util.Md5Util;

import com.minio.util.ShardFileUtil;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @Author WangXiaoLong
 * @Date 2022/6/24 15:59
 * @Version 1.0
 */
@Service
@Slf4j
public class MinioServiceImpl implements MinioService {

    @Autowired
    MinioTemplate minioTemplate;

    @Autowired
    private MinioConfig minioConfig;

    @Resource(name = "jsonRedisTemplate")
    private RedisTemplate<String, Serializable> jsonRedisTemplate;

    private static final String MD5_KEY = "cn:lyf:minio:demo:file:md5List";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 普通分片上传
     */
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


    @Transactional
    @SneakyThrows
    @Override
    public void uploadShard(MultipartFile file, HttpSession session, String fileType) {
        // 上传过程中出现异常，状态码设置为50000
        if (file == null) {
            session.setAttribute("status", StatusCode.FAILURE);
            throw new Exception(StatusCode.FAILURE.getMessage());
        }
        long fileSize = file.getSize();
        session.setAttribute("fileSize", fileSize);
        InputStream FileInputStream = file.getInputStream();
        String md5BucketName = Md5Util.calculateMd5(FileInputStream);
        session.setAttribute("md5BucketName", md5BucketName);
        if (checkFileExists(md5BucketName).getCode().equals(StatusCode.NOT_FOUND.getCode())) {//当前文件不存在可以上传
            //创建名为{md5}的bucket
            minioTemplate.makeBucket(md5BucketName);
            //开始分片
            long shardSize = 5 * 1024 * 1024L; //分片大小
            List<InputStream> inputStreams = ShardFileUtil.splitMultipartFileInputStreams(file, shardSize);
            long shardCount = inputStreams.size(); //总片数
            if (shardCount > 10000) {
                throw new RuntimeException("Total parts count should not exceed 10000");
            }
            log.info("总块数：" + shardCount);
            CountDownLatch countDownLatch = new CountDownLatch((int) shardCount);
            try {
                log.info("开始分片上传:" + file.getOriginalFilename());
                for (int i = 0; i < shardCount; i++) {
                    executorService.execute(new BranchThread(inputStreams.get(i), md5BucketName, i + 1, session, countDownLatch, shardCount, minioTemplate));
                }
            } catch (Exception e) {
                log.error("线程上传出现异常{}", e.getMessage());
            } finally {
                //关闭线程池
                executorService.shutdown();
            }
            countDownLatch.await();
            log.info("所有分片上传完成");
            session.setAttribute("status", StatusCode.ALL_CHUNK_UPLOAD_SUCCESS.getCode());
            //TODO 合并分片

            MergeShardArgs mergeShardArgs = new MergeShardArgs((int) shardCount, file.getOriginalFilename(), md5BucketName, fileType, fileSize);
            try {
                mergeShard(mergeShardArgs);
                log.info("文件上传成功 {} ", file.getName());
                session.setAttribute("status", StatusCode.ALL_CHUNK_MERGE_SUCCESS.getCode());
            } catch (Exception e) {
                log.error("分片合并失败：{}", e.getMessage());
            }
        } else {
            //文件存在了
            log.error("出现异常: {}", StatusCode.FOUND.getMessage());
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
                    jsonRedisTemplate.boundHashOps(MD5_KEY).put(fileMd5, url);

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
    public R mergeShard(MergeShardArgs mergeShardArgs) {
        R r = new R();
        Integer shardCount = mergeShardArgs.getShardCount();
        String fileName = mergeShardArgs.getFileName();
        Long fileSize = mergeShardArgs.getFileSize();
        String md5 = mergeShardArgs.getMd5();
        String fileType = mergeShardArgs.getFileType();
        try {
            //TODO 断点续传
            // 查询片数据
            List<String> objectNameList = minioTemplate.listObjectNames(md5);
            //查询的服务器的分片和传入的分片不同
            if (shardCount != objectNameList.size()) {
                // 失败
                r.put("status", StatusCode.FAILURE.getCode());
            } else {
                // 开始合并请求
                String targetBucketName = minioConfig.getBucketName();
//                String filenameExtension = StringUtils.getFilenameExtension(fileName);
                String fileNameWithoutExtension = UUID.randomUUID().toString();
                //TODO 拼接文件名
                String objectName = fileNameWithoutExtension + "-" + fileName;
                //合并
                minioTemplate.composeObject(md5, targetBucketName, objectName);

                log.info("桶：{} 中的分片文件，已经在桶：{},文件 {} 合并成功", md5, targetBucketName, objectName);

                // 合并成功之后删除对应的临时桶
                minioTemplate.removeBucket(md5, true);
                log.info("删除桶 {} 成功", md5);
                String url = minioTemplate.getPresignedObjectUrl(targetBucketName, objectName);
                // 存入redis中
                jsonRedisTemplate.boundHashOps(MD5_KEY).put(md5, url);
                log.info("文件合并全部成{}并存入缓存", objectName);

            }
        } catch (Exception e) {
            log.error("合并失败:{}", e.getMessage());
            // 失败
            r.put("status", StatusCode.FAILURE.getCode());
        }
        return r;
    }

    @Override
    public R checkFileExists(String md5) {
        R r = new R();
        if (ObjectUtils.isEmpty(md5)) {
            return r.put("code", StatusCode.NULL_ARGS.getCode());
        }
        // 查询
        String url = (String) jsonRedisTemplate.boundHashOps(MD5_KEY).get(md5);

        // 文件不存在
        if (ObjectUtils.isEmpty(url)) {
            return r.put("code", StatusCode.NOT_FOUND.getCode());
        }
        //文件存在
        return r.put("code", StatusCode.FOUND.getCode());
    }

    @Slf4j
    private static class BranchThread implements Runnable {
        /**
         * 文件流
         */
        private final InputStream inputStream;

        /**
         * md5名
         */
        private final String md5BucketName;

        /**
         * 当前片数
         */
        private final Integer curIndex;


        /**
         * 返回给前端的Session
         */
        private final HttpSession session;

        /**
         * 计数等待线程执行完成
         */
        private final CountDownLatch countDownLatch;

        /**
         * 总片数
         * */
        private final long shardCount;

        /**
         * template
         */
        private final MinioTemplate minioTemplate;

        public BranchThread(InputStream inputStream, String md5BucketName, Integer curIndex, HttpSession session, CountDownLatch countDownLatch, long shardCount, MinioTemplate minioTemplate) {
            this.inputStream = inputStream;
            this.md5BucketName = md5BucketName;
            this.curIndex = curIndex;
            this.session = session;
            this.countDownLatch = countDownLatch;
            this.shardCount = shardCount;
            this.minioTemplate = minioTemplate;
        }

        @SneakyThrows
        @Override
        public void run() {
            try {
                double uploadPercent = (double) (curIndex * 100) / shardCount;
                String curIndexName = String.valueOf(curIndex);
                //设置百分比
                session.setAttribute("uploadPercent", uploadPercent);
                log.info("uploadPercent:{}", uploadPercent);
                //设置上传文件大小
                session.setAttribute("uploadSzie", inputStream.available() + "MB");
                log.info("uploadSzie：{}", inputStream.available() + "MB");
                OssFile ossFile = minioTemplate.putChunkObject(inputStream, md5BucketName, curIndexName);
                log.info("{} 上传成功 {}", curIndexName, ossFile);

            } catch (Exception e) {
                log.error("线程上传分片异常{}", e.getMessage());
            } finally {
                countDownLatch.countDown();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        log.error("IO流关闭异常{}", e.getMessage());
                    }
                }
            }


        }
    }


}
