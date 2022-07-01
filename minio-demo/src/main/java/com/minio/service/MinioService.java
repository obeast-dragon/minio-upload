package com.minio.service;


import com.minio.entity.MergeShardArgs;
import com.minio.entity.ResponseEntry;
import org.springframework.web.multipart.MultipartFile;


import java.util.Map;

public interface MinioService  {


    /**
     * 文件上传，适合大文件，集成了分片上传
     * @param file 前台文件；
     * @param res 前后端共享数据
     * @param fileType 文件类型
     */
    void uploadShard(MultipartFile file, ResponseEntry res, String fileType);

    /**
     * 文件合并
     *
     * @param mergeShardArgs 合并参数
     */
    void mergeShard(MergeShardArgs mergeShardArgs);



    /**
     * 根据文件大小和文件的md5校验文件是否存在
     * 暂时使用Redis实现，后续需要存入数据库
     * 实现秒传接口
     *
     * @param md5 文件的md5
     * @return 操作是否成功
     */
    boolean isFileExists(String md5);



}
