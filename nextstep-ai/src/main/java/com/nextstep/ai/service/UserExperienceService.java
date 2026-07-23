package com.nextstep.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.ai.entity.UserExperience;
import com.nextstep.ai.mapper.UserExperienceMapper;
import com.nextstep.common.core.ResultCode;
import com.nextstep.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserExperienceService {

    private final UserExperienceMapper experienceMapper;

    public List<UserExperience> list(Long userId) {
        return experienceMapper.selectList(new LambdaQueryWrapper<UserExperience>()
                .eq(UserExperience::getUserId, userId)
                .orderByDesc(UserExperience::getEndDate)
                .orderByDesc(UserExperience::getId));
    }

    public void delete(Long userId, Long id) {
        UserExperience exp = experienceMapper.selectById(id);
        if (exp == null || !exp.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "经历不存在");
        }
        experienceMapper.deleteById(id);
    }
}
