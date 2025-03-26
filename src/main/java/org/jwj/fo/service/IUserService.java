package org.jwj.fo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jwj.fo.dto.LoginFormDTO;
import org.jwj.fo.dto.Result;
import org.jwj.fo.entity.Blog;
import org.jwj.fo.entity.User;

import javax.servlet.http.HttpSession;
import java.util.List;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result logout();

    Result queryUserById(Long userId);


    Result sign();

    Result signCount();
}
