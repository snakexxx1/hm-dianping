package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;


/**
 * 定义逻辑过期
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;  //此data就是你想存入的数据
}
