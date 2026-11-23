package com.tva.startrail.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "绑定邮箱")
    private String email;

    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
