package com.nextstep.ai.service;

import com.nextstep.ai.client.ChatModels;

import java.util.List;
import java.util.Map;

/**
 * 对话补缺画像可用的 Tools（OpenAI Function Calling 格式）
 * 当前只暴露写画像 + 加经历两个动作，故意保持小而稳。
 */
public final class ChatToolRegistry {

    private ChatToolRegistry() {}

    public static final String TOOL_WRITE_PROFILE  = "writeProfile";
    public static final String TOOL_ADD_EXPERIENCE = "addExperience";

    public static List<ChatModels.Tool> tools() {
        return List.of(writeProfile(), addExperience());
    }

    private static ChatModels.Tool writeProfile() {
        ChatModels.Function f = new ChatModels.Function();
        f.setName(TOOL_WRITE_PROFILE);
        f.setDescription("更新用户画像中的若干字段。只传入用户在本轮明确告诉你的字段，其他字段不要捏造。");
        f.setParameters(Map.of(
                "type", "object",
                "properties", Map.ofEntries(
                        prop("currentSchool",      "string", "当前就读院校名称，如 清华大学"),
                        prop("schoolLevel",        "string", "院校层次，枚举：C9 / 985 / 211 / DOUBLE_FIRST / REGULAR / COLLEGE"),
                        prop("currentMajor",       "string", "当前专业"),
                        prop("majorCategory",      "string", "学科门类，如 工学 / 理学 / 经济学 / 管理学 / 法学 / 文学 / 教育学 / 艺术学 / 医学 / 哲学 / 历史学 / 农学"),
                        prop("degreeType",         "string", "学历，枚举：BACHELOR / MASTER / DOCTOR"),
                        prop("gradeYear",          "integer", "年级：1-4 本科，5-7 硕博"),
                        prop("gpa",                "number", "GPA 数值（按用户填写的制式原值）"),
                        prop("gpaScale",           "integer", "GPA 制式：4 / 5 / 100"),
                        prop("classRankPct",       "number", "班级排名百分位 0-100（如前 10% 填 10）；用户可能说\"前 5%\"\"班级第 2\"等"),
                        prop("englishLevel",       "string", "语言等级，枚举：CET4/CET6/TEM4/TEM8/IELTS/TOEFL/JLPT_N1..N5/TOPIK1/TOPIK2/OTHER/NONE"),
                        prop("englishScore",       "integer", "语言考试分数；雅思请乘 10 后取整（6.5 → 65）"),
                        prop("targetPaths",        "string", "目标路径，逗号分隔：PG=考研、CS=考公、EM=就业，如 PG,EM"),
                        prop("preferredRegions",   "string", "偏好城市，逗号分隔，如 北京,上海"),
                        prop("preferredIndustries","string", "偏好行业，逗号分隔，如 互联网,金融,教育（仅 targetPaths 含 EM 时收集）"),
                        prop("salaryExpectation",  "integer", "期望月薪（元），仅当目标含 EM 时收集"),
                        prop("riskAppetite",       "integer", "风险偏好 1-5（保守 → 激进）"),
                        prop("monthlyBudget",      "integer", "每月可承受备考开销（元），仅当目标含 PG/CS 时收集"),
                        prop("currentStatus",      "string", "目前阶段：IN_SCHOOL/PREPARING/JOB_HUNTING/GRADUATED/EMPLOYED"),
                        prop("interests",          "string", "用户兴趣描述（自由文本，可选）"),
                        prop("strengths",          "string", "用户自述的优势（自由文本，可选）"),
                        prop("weaknesses",         "string", "用户自述的劣势/短板（自由文本，可选）")
                ),
                "additionalProperties", false,
                "required", List.of()
        ));
        return new ChatModels.Tool(f);
    }

    private static ChatModels.Tool addExperience() {
        ChatModels.Function f = new ChatModels.Function();
        f.setName(TOOL_ADD_EXPERIENCE);
        f.setDescription("追加一条经历到用户的经历列表（实习/项目/奖项/科研/论文/竞赛）。一次只加一条，多条请多次调用。");
        f.setParameters(Map.of(
                "type", "object",
                "properties", Map.of(
                        "type",        Map.of("type", "string",
                                "enum", List.of("INTERNSHIP", "PROJECT", "AWARD", "RESEARCH", "PAPER", "COMPETITION"),
                                "description", "经历类型"),
                        "title",       Map.of("type", "string", "description", "公司/项目/奖项名称"),
                        "role",        Map.of("type", "string", "description", "职位/角色（可选）"),
                        "startDate",   Map.of("type", "string", "description", "开始日期 YYYY-MM 或 YYYY（可选）"),
                        "endDate",     Map.of("type", "string", "description", "结束日期 YYYY-MM 或 YYYY（可选）"),
                        "description", Map.of("type", "string", "description", "经历摘要（可选）")
                ),
                "required", List.of("type", "title")
        ));
        return new ChatModels.Tool(f);
    }

    private static Map.Entry<String, Object> prop(String name, String type, String desc) {
        return Map.entry(name, Map.of("type", type, "description", desc));
    }
}
