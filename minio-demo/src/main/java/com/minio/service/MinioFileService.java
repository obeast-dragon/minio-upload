package com.minio.service;


import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/29 17:49
 * @Version 1.0
 */
public interface MinioFileService {

    /**
     * 文件上传，适合大文件，集成了分片上传
     * @param req 前台数据
     */
    Map<String, Object> upload(HttpServletRequest req);

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
    Map<String, Object> merge(Integer shardCount, String fileName, String md5, String fileType,
                              Long fileSize);



    /**
     * 根据文件大小和文件的md5校验文件是否存在
     * 暂时使用Redis实现，后续需要存入数据库
     * 实现秒传接口
     *
     * @param md5 文件的md5
     * @return 操作是否成功
     */
    Map<String, Object> checkFileExists(String md5);

}
