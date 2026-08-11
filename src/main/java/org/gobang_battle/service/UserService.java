package org.gobang_battle.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.gobang_battle.BusinessException;
import org.gobang_battle.mapper.UserMapper;
import org.gobang_battle.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    //用户登录业务
    public User login(String username, String password) {
        //根据用户名查询user
        User user = userMapper.selectByUsername(username);
        //用户名出错或者用户不存在
        if (user == null) {
            log.error("用户登陆失败，用户名不存在，username: {}", username);
            throw new BusinessException("用户名不存在");
        }
        //密码输入错误
        if (!user.getPassword().equals(password)) {
            log.error("用户登陆失败，密码错误，username: {}", username);
            throw new BusinessException("密码错误");
        }
        log.info("用户登陆成功：{}", username);
        return user;
    }
    //用户注册业务
    public User register(String username, String password) {
        User oldUser = userMapper.selectByUsername(username);
        if (oldUser != null) {
            log.error("用户注册失败，用户名已存在，username: {}", username);
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        //设置默认值
        user.setScore(1000);
        user.setTotalCount(0);
        user.setWinCount(0);
        userMapper.insertUser(user);
        log.info("用户注册成功：{}", username);
        return user;
    }
}
