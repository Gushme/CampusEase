package com.CampusEase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.CampusEase.dto.LoginFormDTO;
import com.CampusEase.dto.Result;
import com.CampusEase.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm);
}
