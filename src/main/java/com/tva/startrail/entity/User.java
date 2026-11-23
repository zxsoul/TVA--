package com.tva.startrail.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /**
     * 登录账号（唯一）
     */
    private String username;

    /**
     * 昵称（可重复，缺省 = username）
     */
    private String nickname;

    /**
     * 绑定邮箱（唯一，一邮箱一账号）
     */
    private String email;

    /**
     * 密码哈希（BCrypt）
     */
    private String password;
}
