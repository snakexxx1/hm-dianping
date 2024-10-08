package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * 返回值实体类
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
