package com.minio.entity;

/**
 * @Author WangXiaoLong
 * @Date 2022/7/1 13:47
 * @Version 1.0
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息体
 *
 * @author Lenovo
 * @date 2022/5/6
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageVo {
    /**
     * 客户端id
     */
    private String clientId;
    /**
     * 传输数据体(json)
     */
    private String data;
}
