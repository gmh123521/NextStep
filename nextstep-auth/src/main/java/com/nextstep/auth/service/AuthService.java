package com.nextstep.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.auth.dto.LoginRequest;
import com.nextstep.auth.dto.LoginResponse;
import com.nextstep.auth.dto.RegisterRequest;
import com.nextstep.auth.entity.User;
import com.nextstep.auth.mapper.UserMapper;
import com.nextstep.common.core.ResultCode;
import com.nextstep.common.exception.BizException;
import com.nextstep.framework.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Long register(RegisterRequest req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.USER_ALREADY_EXISTS);
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() == null ? req.getUsername() : req.getNickname());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setStatus(0);
        u.setRole("USER");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(u);
        return u.getId();
    }

    public LoginResponse login(LoginRequest req) {
        User u = userMapper.selectByUsernameWithPassword(req.getUsername());
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new BizException(ResultCode.USER_PASSWORD_ERROR);
        }
        if (u.getStatus() != null && u.getStatus() == 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        String token = jwtTokenProvider.generate(u.getId(), u.getUsername(), u.getRole());
        return new LoginResponse(token, jwtTokenProvider.getExpireMillis(), u.getUsername());
    }
}
