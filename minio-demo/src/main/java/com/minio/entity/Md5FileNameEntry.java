package com.minio.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * @Author WangXiaoLong
 * @Date 2022/6/29 18:58
 * @Version 1.0
 */

@Data
@TableName("md5_name")
public class Md5FileNameEntry {
    /**
     * 主键ID
     * */
    @TableId
    private Long id;

    /**
     * mad文件名
     * */
    private String md5FileName;

    /**
     * 文件的路径
     * */
    private String url;

    public Md5FileNameEntry(String md5FileName, String url) {
        this.md5FileName = md5FileName;
        this.url = url;
    }
}
