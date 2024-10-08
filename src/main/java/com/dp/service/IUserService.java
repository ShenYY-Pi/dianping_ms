package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();
}
