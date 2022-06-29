package com.minio.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/24 17:14
 * @Version 1.0
 */
@Data
@AllArgsConstructor
public class MergeShardArgs {
    /**
     * 文件合并参数
     *
     * @param shardCount 分片总数
     * @param fileName   文件名
     * @param md5        文件的md5
     * @param fileType   文件类型
     * @param fileSize   文件大小
     * @return 分片合并的状态
     */
    private Integer shardCount;
    private String fileName;
    private String md5;
    private String fileType;
    private Long fileSize;

}
