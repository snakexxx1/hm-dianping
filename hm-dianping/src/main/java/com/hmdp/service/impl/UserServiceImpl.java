package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

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
        session.setAttribute("code",code);

        //5.发送验证码
            //实现起来太麻烦，要调用阿里云等工具，不是我们的重点 使用日志记录，假设发送成功
        log.info("发送验证码成功，{}",code);

        //返回统用结果对象
        return Result.ok();
    }
}
