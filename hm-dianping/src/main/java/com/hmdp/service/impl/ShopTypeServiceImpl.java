package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 添加店铺分类的redis缓存
     * @return
     */
    public Result queryByType() {
        //1.从redis查询店铺分类缓存
        String shopTypeJson = stringRedisTemplate.opsForValue().get("Shop-Type");
        //2.判断是否存在缓存
          if(StrUtil.isNotBlank(shopTypeJson)) {
              //json转list
              List<ShopType> jsonList =  JSONUtil.toList(shopTypeJson,ShopType.class);
              //3.存在，返回店铺分类
            return Result.ok(jsonList);
          }
        //4.不存在，查询数据库
        List<ShopType> shopTypesByMysql = query().orderByAsc("sort").list();
        //数据库不存在
        if(shopTypesByMysql == null){
            return Result.fail("没有商品类别");
        }
        //5.存在，将店铺分类数据存入redis中
        stringRedisTemplate.opsForValue().set("shop-type",JSONUtil.toJsonStr(shopTypesByMysql));
        //6.返回信息
        return Result.ok(shopTypesByMysql);
    }
}
