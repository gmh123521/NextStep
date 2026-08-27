package com.nextstep.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @TableField(select = false)
    private String password;

    private String nickname;

    private String email;

    private String phone;

    /** 0=正常 1=禁用 */
    private Integer status;

    /** 角色：USER=普通用户 ADMIN=管理员 */
    private String role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
