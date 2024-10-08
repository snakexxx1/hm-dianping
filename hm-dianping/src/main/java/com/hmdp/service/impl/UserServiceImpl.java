package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送短信验证码并保存验证码
     * @param phone
     * @param session
     * @return
     */
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {

          //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3.符合，生成验证码
          //引用糊涂工具类里面的工具RandomUtil的随机生成器,6代表随机生成数字为6的数字
        String code = RandomUtil.randomNumbers(6);

        //4.保存验证码到session  .setAttribute可以保存到session
        //session.setAttribute("code",code);
         //5.保存验证码到Redis
        //设计key的前缀名 和 有效期2分钟
        stringRedisTemplate.opsForValue().set( LOGIN_CODE_KEY + phone,code , LOGIN_CODE_TTL , TimeUnit.MINUTES);


        //5.发送验证码
            //实现起来太麻烦，要调用阿里云等工具，不是我们的重点 使用日志记录，假设发送成功
        log.info("发送验证码成功，{}",code);

        //返回统用结果对象
        return Result.ok();
    }

    /**
     * 实现登录功能
     * @param loginForm
     * @param session
     * @return
     */
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        //2.校验验证码
       // Object cacheCode = session.getAttribute("code");//取出session存的验证码
        //改为从redis中校验 .get
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();  //前端提交的code

        //判断前端传的验证码是否为空或者过期，然后比较（加！就是不一致）
        if(cacheCode == null || !cacheCode.equals(code)){
            //3.不一致，报错
            return Result.fail("验证码错误");
        }

        //4.一致，根据手机号查询用户
        //mybatisPlus query就是select * from user，eq就是比较 one就是查询一个
        User user = query().eq("phone", phone).one();

        //5.判断用户是否存在
         if(user == null){
             //6.不存在，创建新用户并保存
             //根据手机号创建（其余都可以默认随机）
           user = createUserWithPhone(phone);
         }

        //7.保存用户信息到session中
       // session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        //7.保存用户信息到Redis中
        //7.1随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2将user对象转换为HashMap存储
        UserDTO userDTO =  BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        //7.3存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey , userMap);
        //7.4设置token有效期
        stringRedisTemplate.expire(tokenKey , LOGIN_USER_TTL, TimeUnit.MINUTES);

         //8.返回token
        return Result.ok(token);
    }


    //根据手机号创建
    private User createUserWithPhone(String phone) {
        //1.创建用户
        User user =new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX  + RandomUtil.randomString(10));
        //2.保存用户
        save(user); //mybatisPlus的方法
        return user;
    }


    /**
     * 实现签到功能
     * @return
     */
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        //now.format 将当前日期时间对象now格式化为指定格式字符串
        //DateTimeFormatter.ofPattern 日期时间格式化器
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();

    }

    /**
     * 实现签到统计
     * @return
     */
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);

    }
}
