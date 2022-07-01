package com.minio.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minio.entity.Md5FileNameEntry;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author WangXiaoLong
 * @Date 2022/6/29 18:42
 * @Version 1.0
 */
@Mapper
public interface Md5FileNameDao extends BaseMapper<Md5FileNameEntry> {

}
