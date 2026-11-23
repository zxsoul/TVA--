package com.tva.startrail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tva.startrail.entity.User;
import com.tva.startrail.mapper.UserMapper;
import com.tva.startrail.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username), false);
    }

    @Override
    public User getByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email), false);
    }
}
