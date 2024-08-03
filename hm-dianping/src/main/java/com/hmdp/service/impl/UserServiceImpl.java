package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;


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
        Object cacheCode = session.getAttribute("code");//取出session存的验证码
        String code = loginForm.getCode();  //前端提交的code

        //判断前端传的验证码是否为空或者过期，然后比较（加！就是不一致）
        if(cacheCode == null || !cacheCode.toString().equals(code)){
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
        session.setAttribute("user",user);
        return Result.ok();
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
}
