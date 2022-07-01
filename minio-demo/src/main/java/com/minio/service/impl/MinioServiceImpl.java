package com.minio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minio.config.MinioConfig;

import com.minio.dao.Md5FileNameDao;
import com.minio.entity.*;
import com.minio.exception.StatusCode;
import com.minio.service.MinioService;
import com.minio.template.MinioTemplate;
import com.minio.util.Md5Util;

import com.minio.util.ShardFileUtil;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;


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
    MinioConfig minioConfig;

    @Autowired
    Md5FileNameDao md5FileNameDao;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);


    @SneakyThrows
    @Override
    public void uploadShard(MultipartFile file, ResponseEntry res, String fileType) {
        // 上传过程中出现异常，状态码设置为50000
        boolean stopStatus = true;
        if (file == null) {
            res.put("status", StatusCode.FAILURE);
            throw new Exception(StatusCode.FAILURE.getMessage());
        }
        Long fileSize = file.getSize();
        //TODO 上传的状态
        InputStream FileInputStream = file.getInputStream();
        //TODO md5要要修改
        String md5BucketName = Md5Util.calculateMd5(FileInputStream) + "-" +  file.getOriginalFilename();
        String fileName = file.getOriginalFilename();
        res.put("md5BucketName", md5BucketName);
        //分片大小
        long shardSize = 5 * 1024 * 1024L;
        //开始分片
        List<InputStream> inputStreams = ShardFileUtil.splitMultipartFileInputStreams(file, shardSize);
        long shardCount = inputStreams.size(); //总片数
        //封装合并参数
        MergeShardArgs mergeShardArgs = new MergeShardArgs((int) shardCount, fileName, md5BucketName, fileType, fileSize);
        boolean fileExists = isFileExists(md5BucketName);
        boolean bucketExists = minioTemplate.bucketExists(md5BucketName);
        //当前文件不存在DB和minio  可以正常分片上传
        if (!fileExists && !bucketExists) {
            try {
                //创建名为{md5}的bucket
                minioTemplate.makeBucket(md5BucketName);
                //TODO 合并分片
                uploadJob(shardCount, inputStreams, res, stopStatus, md5BucketName, fileName);
                //开始合并
                mergeShard(mergeShardArgs);
                log.info("文件上传成功 {} ", file.getOriginalFilename());
                res.put("status", StatusCode.ALL_CHUNK_MERGE_SUCCESS.getCode());
            } catch (Exception e) {
                log.error("分片合并失败：{}", e.getMessage());
            }
        }
        /**
         * 如果文件存在;
         *  1、存在DB minio == null 上传完成秒传
         *  2、存在minio  DB == null
         * 先看临时桶在不在
         *  1、在;断点上传
         *  2、在;没合并
         * */
        else if (fileExists && !bucketExists) {
            //1、存在DB minio == null 秒传
            //设置百分比
            res.put("uploadPercent", 100);
            log.info("uploadPercent:{}", 100);
            //设置上传文件大小
            res.put("uploadSize", fileSize);
            log.info("uploadSize：{}", fileSize);
            log.info("{} 秒传成功", fileName);
        }
        else if (!fileExists) {
            //2、存在minio  DB == null
//         *  1、在;断点上传
//         *  2、在;没合并
            List<String> objectNames = minioTemplate.listObjectNames(md5BucketName);
            //临时桶在; 断点上传
            if (objectNames.size() == shardCount) {
                //没有合并: 合并秒传
                mergeShard(mergeShardArgs);
                //设置百分比
                res.put("uploadPercent", 100);
                log.info("uploadPercent:{}", 100);
                //设置上传文件大小
                res.put("uploadSize", fileSize);
                log.info("uploadSize：{}", fileSize);
                log.info("{} 秒传成功", fileName);
            } else {
                //断点上传
                List<String> containStr = containList(objectNames, shardCount);
                System.out.println(containStr);
                CountDownLatch countDownLatch = new CountDownLatch(containStr.size());
                try {
                    log.info("开始断点分片上传:" + fileName);
                    for (int i = 0; i < containStr.size(); i++) {
                        stopStatus = (boolean) res.get("stopStatus");
                        if (stopStatus) {
                            int c = Integer.parseInt(containStr.get(i));
                            executorService.execute(new BranchThread(inputStreams.get(c - 1), md5BucketName, c, res, countDownLatch, shardCount, stopStatus, minioTemplate));
                        } else {
                            executorService.shutdown();
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("线程上传出现异常{}", e.getMessage());
                } finally {
                    //关闭线程池
                    executorService.shutdown();
                }
                countDownLatch.await();
                log.info("所有分片上传完成");
                res.put("status", StatusCode.ALL_CHUNK_UPLOAD_SUCCESS.getCode());

                mergeShard(mergeShardArgs);
                log.info("文件上传成功 {} ", fileName);
            }
            res.put("status", StatusCode.ALL_CHUNK_MERGE_SUCCESS.getCode());
        } else {
            //出现异常
            log.error("出现异常: {}", StatusCode.FOUND.getMessage());
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

    private void uploadJob(long shardCount, List<InputStream> inputStreams, ResponseEntry res, boolean stopStatus, String md5BucketName, String fileName) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch((int) shardCount);
        if (shardCount > 10000) {
            throw new RuntimeException("Total parts count should not exceed 10000");
        }
        log.info("总片数：" + shardCount);
        try {
            log.info("开始分片上传:" + fileName);
            for (int i = 0; i < shardCount; i++) {
                stopStatus = (boolean) res.get("stopStatus");
                if (stopStatus) {
                    executorService.execute(new BranchThread(inputStreams.get(i), md5BucketName, i + 1, res, countDownLatch, shardCount, stopStatus, minioTemplate));
                } else {
                    executorService.shutdown();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("线程上传出现异常{}", e.getMessage());
        } finally {
            //关闭线程池
            executorService.shutdown();
        }
        countDownLatch.await();
        log.info("所有分片上传完成");
        res.put("status", StatusCode.ALL_CHUNK_UPLOAD_SUCCESS.getCode());
    }


    @Transactional
    @Override
    public void mergeShard(MergeShardArgs mergeShardArgs) {
        Integer shardCount = mergeShardArgs.getShardCount();
        String fileName = mergeShardArgs.getFileName();
        String md5 = mergeShardArgs.getMd5();
        try {
            List<String> objectNameList = minioTemplate.listObjectNames(md5);
            //查询的服务器的分片和传入的分片不同
            if (shardCount != objectNameList.size()) {
                // 失败
                log.error("服务器的分片{}和传入的分片不同{}", shardCount, objectNameList.size());
                throw new Exception("服务器的分片和传入的分片不同");
            } else {
                // 开始合并请求
                String targetBucketName = minioConfig.getBucketName();
                String fileNameWithoutExtension = UUID.randomUUID().toString();
                String objectName = fileNameWithoutExtension + "-" + fileName;
                //合并
                minioTemplate.composeObject(md5, targetBucketName, objectName);

                log.info("桶：{} 中的分片文件，已经在桶：{},文件 {} 合并成功", md5, targetBucketName, objectName);
                String url = minioTemplate.getPresignedObjectUrl(targetBucketName, objectName);
                // 合并成功之后删除对应的临时桶
                minioTemplate.removeBucket(md5, true);
                log.info("删除桶 {} 成功", md5);
                // 存入DB中
                Md5FileNameEntry md5FileNameEntry = new Md5FileNameEntry(md5, url);
                md5FileNameDao.insert(md5FileNameEntry);
                log.info("文件合并成{}并存入DB", md5FileNameEntry);
            }
        } catch (Exception e) {
            // 失败
            log.error("合并失败:{}", e.getMessage());
        }
    }

    @SneakyThrows
    @Override
    public boolean isFileExists(String md5) {
        if (ObjectUtils.isEmpty(md5)) {
            log.error("参数md5为空");
            throw new Exception("参数md5为空");
        }
        // 查询
        Md5FileNameEntry md5FileNameEntry = md5FileNameDao.selectOne(new QueryWrapper<Md5FileNameEntry>()
                .eq("md5_file_name", md5)
        );
        /**
         * 文件不存在 false
         * 文件存在 true
         * */
        return md5FileNameEntry != null;
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
         * 返回给前端的res
         */
        private final ResponseEntry res;

        /**
         * 计数等待线程执行完成
         */
        private final CountDownLatch countDownLatch;

        /**
         * 总片数
         */
        private final long shardCount;

        /**
         * 暂停状态
         */
        private final boolean stopStatus;
        /**
         * template
         */
        private final MinioTemplate minioTemplate;

        public BranchThread(InputStream inputStream, String md5BucketName, Integer curIndex, ResponseEntry res, CountDownLatch countDownLatch, long shardCount, boolean stopStatus, MinioTemplate minioTemplate) {
            this.inputStream = inputStream;
            this.md5BucketName = md5BucketName;
            this.curIndex = curIndex;
            this.res = res;
            this.countDownLatch = countDownLatch;
            this.shardCount = shardCount;
            this.stopStatus = stopStatus;
            this.minioTemplate = minioTemplate;
        }

        @SneakyThrows
        @Override
        public void run() {
            try {
                if (stopStatus) {
                    Long uploadPercent =  ((curIndex * 100) / shardCount);
                    String curIndexName = String.valueOf(curIndex);
                    //设置百分比
                    res.put("uploadPercent", uploadPercent);
                    log.info("uploadPercent:{}", uploadPercent);
                    //设置上传文件大小
                    res.put("uploadSize", inputStream.available());
                    log.info("uploadSize：{}", inputStream.available());
                    OssFile ossFile = minioTemplate.putChunkObject(inputStream, md5BucketName, curIndexName);
                    log.info("上传成功 {}", ossFile);
                } else {
                    executorService.shutdown();
                }

            } catch (Exception e) {
                log.error("线程上传分片异常{}", e.getMessage());
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}
