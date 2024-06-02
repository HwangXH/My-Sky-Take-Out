package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录，和传统的账号密码不同
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前微信用户的openid
        String openId = getOpenId(userLoginDTO.getCode());

        //判断openid是否为空，如果为空表示失败，抛出异常
        if(openId==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //如果不为空，则进一步判断当前用户是否是小程序的新用户，如果是新用户则完成自动注册
        User user = userMapper.getByOpenId(openId);
        if(user==null){
            //还有很多字段没有填充，但是暂时还是未知的，后续由用户在个人中心完善
            // TODO 为什么不使用AutoFill
            //不使用AutoFill是因为自动填充是在填充一些实体类时使用的
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回用户对象
        return user;
    }

    /**
     * 调用微信接口服务，获取用户的openid（只服务于微信登录）
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        //小程序登录时提供的一个短期有效的code，在DTO中
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString("openid");
    }
}
