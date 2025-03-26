package org.jwj.fo.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jwj.fo.dto.LoginFormDTO;
import org.jwj.fo.dto.Result;
import org.jwj.fo.dto.UserDTO;
import org.jwj.fo.entity.Blog;
import org.jwj.fo.entity.User;
import org.jwj.fo.entity.UserInfo;
import org.jwj.fo.service.IBlogService;
import org.jwj.fo.service.IUserInfoService;
import org.jwj.fo.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.jwj.fo.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.List;

import static org.jwj.fo.utils.SystemConstants.MAX_PAGE_SIZE;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;


    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        return userService.logout();
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        return Result.ok(UserHolder.getUser());
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        return userService.queryUserById(userId);
    }
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }
    @GetMapping("/signs/count")
    public Result signCount(){
        return userService.signCount();
    }
}
