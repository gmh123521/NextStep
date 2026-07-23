package com.nextstep.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
