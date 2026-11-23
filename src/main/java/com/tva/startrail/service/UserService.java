package com.tva.startrail.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tva.startrail.entity.User;

public interface  UserService extends IService<User> {

    User getByUsername(String username);

    User getByEmail(String email);
}
