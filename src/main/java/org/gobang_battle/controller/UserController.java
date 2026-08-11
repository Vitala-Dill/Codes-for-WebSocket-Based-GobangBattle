package org.gobang_battle.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.gobang_battle.BusinessException;
import org.gobang_battle.game.OnlineUserState;
import org.gobang_battle.model.User;
import org.gobang_battle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public User login(String username, String password, HttpServletRequest request) {
        User user = userService.selectByUsername(username);
        log.info("用户登录中, username: {}", username);
        //获取当前的session，如果没有就创建一个session
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        return userService.login(username, password);
    }

    @PostMapping("/register")
    public User register(String username, String password) {
        log.info("用户注册中, username: {}", username);
        return userService.register(username, password);
    }

    @GetMapping("/userInfo")
    public Object getUserInfo(HttpServletRequest request) {
        log.info("获取用户信息中...");
        //获取session对象，没有的话就返回null
        HttpSession session = request.getSession(false);
        User user = (User)session.getAttribute("user");
        //拿着user对象的属性，去数据库查找最新的User对象
        User newUser = userService.selectByUsername(user.getUsername());
        if (newUser == null) {
            log.error("用户不存在...");
            throw new BusinessException("用户不存在");
        }
        log.info("获取用户信息成功...");
        return newUser;
    }
}
