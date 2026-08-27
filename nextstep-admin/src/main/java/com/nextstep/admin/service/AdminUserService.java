package com.nextstep.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.auth.entity.User;
import com.nextstep.auth.mapper.UserMapper;
import com.nextstep.common.core.PageResult;
import com.nextstep.common.exception.BizException;
import com.nextstep.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 后台用户管理：分页、启用/禁用、角色调整。
 * 注意：User.password 标注了 @TableField(select=false)，MyBatis-Plus 查询自动排除密码列。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    public PageResult<User> page(int pageNum, int pageSize, String keyword, Integer status, String role) {
        validatePage(pageNum, pageSize);
        if (status != null && status != 0 && status != 1) throw new BizException("非法用户状态：" + status);
        if (role != null && !role.isBlank() && !isRole(role)) throw new BizException("非法角色：" + role);
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), q -> q
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword))
                .eq(status != null, User::getStatus, status)
                .eq(StringUtils.hasText(role), User::getRole, role)
                .orderByDesc(User::getId);
        Page<User> p = userMapper.selectPage(Page.of(pageNum, pageSize), w);
        return PageResult.of(p.getTotal(), pageNum, pageSize, p.getRecords());
    }

    /** 启用/禁用：status 0=正常 1=禁用 */
    public void setStatus(Long userId, int status) {
        if (status != 0 && status != 1) throw new BizException("非法用户状态：" + status);
        if (userId == null) throw new BizException("用户 ID 不能为空");
        if (status == 1 && userId.equals(SecurityUtils.currentUserId())) {
            throw new BizException("不能禁用当前登录管理员");
        }
        User u = requireUser(userId);
        u.setStatus(status);
        userMapper.updateById(u);
    }

    /** 调整角色：USER / ADMIN */
    public void setRole(Long userId, String role) {
        if (!isRole(role)) {
            throw new BizException("非法角色：" + role);
        }
        if (userId == null) throw new BizException("用户 ID 不能为空");
        if ("USER".equals(role) && userId.equals(SecurityUtils.currentUserId())) {
            throw new BizException("不能取消当前登录管理员权限");
        }
        User u = requireUser(userId);
        u.setRole(role);
        userMapper.updateById(u);
    }

    private User requireUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException("用户不存在：" + userId);
        return u;
    }

    private boolean isRole(String role) {
        return "USER".equals(role) || "ADMIN".equals(role);
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BizException("分页参数非法：页码必须大于 0，页大小为 1-200");
        }
    }
}
