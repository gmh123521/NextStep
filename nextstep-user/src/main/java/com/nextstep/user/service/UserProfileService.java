package com.nextstep.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.user.dto.UserProfileRequest;
import com.nextstep.user.entity.UserProfile;
import com.nextstep.user.mapper.UserExperienceTypeMapper;
import com.nextstep.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final UserExperienceTypeMapper experienceTypeMapper;

    /** 评分用到的核心字段（基础必填项） */
    private static final List<String> CORE_FIELDS = Arrays.asList(
            "currentSchool", "schoolLevel", "currentMajor", "majorCategory", "degreeType",
            "gradeYear", "gpa", "classRankPct", "englishLevel",
            "targetPaths", "preferredRegions",
            "riskAppetite"
    );

    /** 仅在勾选目标路径含 EM（就业）时才计入完整度的字段 */
    private static final List<String> EM_FIELDS = Arrays.asList(
            "preferredIndustries", "salaryExpectation"
    );

    /** 仅在勾选目标路径含 PG/CS（考研/考公）时才计入完整度的字段 */
    private static final List<String> PGCS_FIELDS = Arrays.asList(
            "monthlyBudget"
    );

    public UserProfile get(Long userId) {
        UserProfile p = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
        if (p == null) return null;
        // 派生字段实时注入
        p.setProfileCompleteness(calcCompleteness(p));
        injectExperienceFlags(p);
        return p;
    }

    /** 从经历表聚合 has_* 标志位 */
    private void injectExperienceFlags(UserProfile p) {
        Set<String> types = new HashSet<>();
        boolean hasCompetitionAward = false;
        try {
            // 1) 按 type 聚合
            for (Map<String, Object> row : experienceTypeMapper.countByTypes(p.getUserId())) {
                Object t = row.get("type");
                if (t != null) types.add(t.toString());
            }
            // 2) AWARD 里如果标题含"竞赛/杯/赛/ICPC/ACM/Kaggle"等关键词，也算作"参加过竞赛"
            //    解决"蓝桥杯省级一等奖"被 LLM 标成 AWARD 但用户期待 has_competition=1 的问题
            if (types.contains("AWARD")) {
                List<String> awardTitles = experienceTypeMapper.titlesByType(p.getUserId(), "AWARD");
                for (String t : awardTitles) {
                    if (t == null) continue;
                    String lower = t.toLowerCase();
                    if (t.contains("竞赛") || t.contains("杯") || t.contains("赛")
                            || lower.contains("icpc") || lower.contains("ccpc")
                            || lower.contains("acm")  || lower.contains("kaggle")
                            || lower.contains("olympiad") || lower.contains("contest")) {
                        hasCompetitionAward = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 经历表不存在或查询失败时静默兜底
        }
        p.setHasInternship(types.contains("INTERNSHIP")  ? 1 : 0);
        p.setHasResearch(types.contains("RESEARCH")      ? 1 : 0);
        p.setHasCompetition((types.contains("COMPETITION") || hasCompetitionAward) ? 1 : 0);
        p.setHasPaper(types.contains("PAPER")            ? 1 : 0);
    }

    @Transactional
    public UserProfile upsert(Long userId, UserProfileRequest req) {
        UserProfile existing = getRaw(userId);
        UserProfile target = existing == null ? new UserProfile() : existing;
        copyNonNull(req, target);
        target.setUserId(userId);
        // upsert 时不再写 profileCompleteness：它是派生字段，由 get() 时实时算
        if (existing == null) {
            userProfileMapper.insert(target);
        } else {
            userProfileMapper.updateById(target);
        }
        // 返回前注入派生字段
        target.setProfileCompleteness(calcCompleteness(target));
        injectExperienceFlags(target);
        return target;
    }

    /** 内部用：直接查数据库不做派生注入（避免循环） */
    private UserProfile getRaw(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
    }

    private void copyNonNull(Object src, Object dest) {
        BeanWrapper wrapper = new BeanWrapperImpl(src);
        Set<String> nullProps = new HashSet<>();
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            if (wrapper.getPropertyValue(pd.getName()) == null) {
                nullProps.add(pd.getName());
            }
        }
        BeanUtils.copyProperties(src, dest, nullProps.toArray(new String[0]));
    }

    int calcCompleteness(UserProfile p) {
        // 动态构建本用户的"分母"字段：核心字段 + 路径条件性字段
        List<String> denominator = new java.util.ArrayList<>(CORE_FIELDS);
        String paths = p.getTargetPaths() == null ? "" : p.getTargetPaths();
        if (paths.contains("EM")) denominator.addAll(EM_FIELDS);
        if (paths.contains("PG") || paths.contains("CS")) denominator.addAll(PGCS_FIELDS);

        int filled = 0;
        for (String name : denominator) {
            try {
                Field f = UserProfile.class.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(p);
                if (v == null) continue;
                if (v instanceof String s && s.isBlank()) continue;
                if (v instanceof BigDecimal bd && bd.signum() == 0) continue;
                filled++;
            } catch (ReflectiveOperationException ignore) {
            }
        }
        return Math.round(100f * filled / denominator.size());
    }
}
