package com.hmdp.utils;


import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    //锁的名称
    private String name;
    //redis工具类
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //释放锁的脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        //setLocation：设置脚本位置 会去ClassPathResource下寻找资源
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        //setResultTyp 配置返回值 Long类型
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    //锁的前缀
    private static final String KEY_PREFIX="lock:";

    //huTol工具类下的uuid toString(true) 可以去掉横线
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示 uuid拼接线程id
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取线程标示Long threadId = Thread.currentThread().getId();
        // 获取锁 setIfAbsent(nx)
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    //基于Lua脚本执行
    @Override
    public void unlock() {
        //调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                //生成集合Collections.singletonList
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());

        }
    }
    /*@Override
    public void unlock() {
        //通过del删除锁stringRedisTemplate.delete(KEY_PREFIX + name);
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标示
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断标示是否一致
        if(threadId.equals(id)) {
            // 释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }*/
