package com.cetide.im.service;

import com.cetide.im.model.Result;
import com.cetide.im.model.User;

public interface UserService {

    /**
     * 注册用户
     *
     * @param userName
     * @param pwd
     * @return
     */
    public Result<User> register(String userName, String pwd);

    /**
     * 执行登录逻辑，登录成功返回 User 对象
     *
     * @param userName
     * @param pwd
     * @return
     */
    public Result<User> login(String userName, String pwd);

}
