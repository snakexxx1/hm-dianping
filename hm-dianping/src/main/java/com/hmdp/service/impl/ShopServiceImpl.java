package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
      @Resource
      private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据id查询商铺缓存
     * @param id
     * @return
     */
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY + id ;
        //1.从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在  StrUtil.isNotBlank工具类判断字符串是否不为空
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在，直接返回
            // JSONUtil.toBean 用于将JSON格式的字符串转换（反序列化）为Java对象
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return  Result.ok(shop);
        }
        //4.不存在，根据id查询数据库
          Shop shop = getById(id);
        //5.不存在，返回错误
       if(shop == null){
           return Result.fail("店铺不存在");
       }
        //6.存在，写入redis中
        // JSONUtil.toJsonStr将一个Java对象（比如一个实体类实例）转换成JSON格式的字符串
       stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop) , CACHE_SHOP_TTL , TimeUnit.MINUTES);
        //7.返回
       return Result.ok(shop);

    }

    /**
     * 更新店铺信息
     * @param shop
     * @return
     */
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
       updateById(shop);
        //2.删除缓存
       stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
