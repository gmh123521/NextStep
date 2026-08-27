package com.nextstep.admin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 后台首页概览统计 */
@Data
public class AdminStats implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private long totalUsers;
    private long activeUsers;
    private long disabledUsers;
    private long adminUsers;

    private long totalSchools;
    private long totalGovPosts;
    private long totalJobPositions;
    private long totalSalaryStats;
}
