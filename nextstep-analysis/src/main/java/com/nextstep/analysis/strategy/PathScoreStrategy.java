package com.nextstep.analysis.strategy;

import com.nextstep.analysis.dto.PathScore;
import com.nextstep.user.entity.UserProfile;

/**
 * 路径评分策略
 */
public interface PathScoreStrategy {

    /** 路径标识 PG / CS / EM */
    String code();

    /** 中文名 */
    String name();

    /** 给出该路径的评分结果 */
    PathScore score(UserProfile profile);
}
