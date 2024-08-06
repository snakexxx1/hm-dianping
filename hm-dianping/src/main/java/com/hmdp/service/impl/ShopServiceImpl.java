package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
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

import static com.hmdp.utils.RedisConstants.*;

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
        //缓存穿透
        //  Shop shop = queryWithPassThrough(id);

       //互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null){
            return Result.fail("店铺不存在！");
        }
        //7.返回
       return Result.ok(shop);

    }

    //互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id ;
        //1.从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在  StrUtil.isNotBlank工具类判断字符串是否不为空
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在，直接返回
            // JSONUtil.toBean 用于将JSON格式的字符串转换（反序列化）为Java对象
            return  JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断是否是空值  空字符串上述的if是不成立的  != null就是空字符串
        if(shopJson != null){
            //返回错误信息
            return null;
        }

        //4.开始实现缓存重建
        //4.1获取互斥锁  //注意此处，缓存的key和锁的key不是同一个值
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2 判断否获取成功
            if(!isLock){
                //4.3 失败，则休眠重试
                Thread.sleep(50);
                //循环重试
                return queryWithMutex(id);
            }
            //4.4 成功，根据id查询数据库
            shop = getById(id);
        //5.不存在，返回错误
        if(shop == null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"" , CACHE_NULL_TTL , TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        //6.存在，写入redis中
        // JSONUtil.toJsonStr将一个Java对象（比如一个实体类实例）转换成JSON格式的字符串
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop) , CACHE_SHOP_TTL , TimeUnit.MINUTES);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
        finally {
            //7.释放互斥锁
            unlock(lockKey);
        }

        //8.返回
        return shop;

    }

    //解决缓存穿透问题
    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id ;
        //1.从redis查询缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在  StrUtil.isNotBlank工具类判断字符串是否不为空
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在，直接返回
            // JSONUtil.toBean 用于将JSON格式的字符串转换（反序列化）为Java对象
            return  JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断是否是空值  空字符串上述的if是不成立的  != null就是空字符串
        if(shopJson != null){
            //返回错误信息
            return null;
        }
        //4.不存在，根据id查询数据库
        Shop shop = getById(id);
        //5.不存在，返回错误
        if(shop == null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"" , CACHE_NULL_TTL , TimeUnit.MINUTES);
            //返回错误信息
            return null;
        }
        //6.存在，写入redis中
        // JSONUtil.toJsonStr将一个Java对象（比如一个实体类实例）转换成JSON格式的字符串
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop) , CACHE_SHOP_TTL , TimeUnit.MINUTES);
        //7.返回
        return shop;

    }

    //设置获取互斥锁 setIfAbsent=redis的setnx方法
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //转为基本数据类型转回
        return BooleanUtil.isTrue(flag);
    }

    //设置释放互斥锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
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
