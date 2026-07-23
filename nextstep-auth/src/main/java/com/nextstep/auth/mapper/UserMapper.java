package com.nextstep.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, username, password, nickname, email, phone, status, created_at, updated_at " +
            "FROM ns_user WHERE username = #{username} LIMIT 1")
    User selectByUsernameWithPassword(String username);
}
