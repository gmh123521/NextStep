package com.nextstep.report.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nextstep.analysis.dto.AnalysisResult;
import com.nextstep.analysis.dto.PathScore;
import com.nextstep.common.exception.BizException;
import com.nextstep.planner.dto.PlanView;
import com.nextstep.planner.entity.UserPlanTask;
import com.nextstep.report.dto.ReportData;
import com.nextstep.user.entity.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 综合报告 PDF 渲染：画像摘要 + 三路径分析 + 推荐路径规划。
 * 复用 planner 的 OpenPDF + STSong-Light CJK 字体方案。
 */
@Slf4j
@Service
public class ReportPdfService {

    private static final Color BRAND      = new Color(64, 158, 255);
    private static final Color BRAND_DARK = new Color(48, 122, 197);
    private static final Color GRAY_LIGHT = new Color(243, 244, 246);
    private static final Color GRAY_TEXT  = new Color(107, 114, 128);
    private static final Color GREEN      = new Color(34, 197, 94);
    private static final Color AMBER      = new Color(245, 158, 11);
    private static final Color RED        = new Color(239, 68, 68);

    private static final BaseFont CN_FONT = createCnFont();

    private static BaseFont createCnFont() {
        try {
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化 PDF 中文字体", e);
        }
    }

    private Font font(float size, int style, Color color) {
        return new Font(CN_FONT, size, style, color);
    }

    public byte[] render(ReportData data) {
        if (data == null || data.getProfile() == null || data.getAnalysis() == null) {
            throw new BizException("报告数据不完整，无法生成 PDF");
        }
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPdfVersion(PdfWriter.VERSION_1_4);
            doc.open();

            renderHeader(doc, data);
            renderProfile(doc, data.getProfile());
            renderAnalysis(doc, data.getAnalysis());
            if (data.getPlan() != null) {
                renderPlan(doc, data.getPlan());
            } else {
                doc.add(divider());
                doc.add(sectionTitle("📅 推荐路径规划"));
                doc.add(new Paragraph("尚未生成推荐路径规划，可在规划页面生成后再次导出报告。",
                        font(10, Font.NORMAL, GRAY_TEXT)));
            }
            renderFooter(doc, data);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            log.error("[report-pdf] 渲染失败: {}", e.getMessage(), e);
            throw new BizException("PDF 生成失败：" + e.getMessage());
        }
    }

    // ── 封面 ──────────────────────────────────────────────────────────────────

    private void renderHeader(Document doc, ReportData data) throws DocumentException {
        Paragraph brand = new Paragraph("NextStep", font(11, Font.BOLD, BRAND));
        doc.add(brand);

        Paragraph title = new Paragraph("综合决策报告", font(24, Font.BOLD, BRAND_DARK));
        title.setSpacingBefore(6);
        title.setSpacingAfter(4);
        doc.add(title);

        String sub = safe(data.getUsername(), "用户") + "  ·  " +
                (data.getGeneratedAt() == null ? "" : data.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        Paragraph subtitle = new Paragraph(sub, font(10, Font.NORMAL, GRAY_TEXT));
        subtitle.setSpacingAfter(14);
        doc.add(subtitle);
        doc.add(divider());
    }

    // ── 个人画像 ──────────────────────────────────────────────────────────────

    private void renderProfile(Document doc, UserProfile p) throws DocumentException {
        doc.add(sectionTitle("👤 个人画像"));

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(14);
        t.setWidths(new float[]{1.2f, 1.8f, 1.2f, 1.8f});

        addRow(t, "院校", p.getCurrentSchool(), "层次", p.getSchoolLevel());
        addRow(t, "专业", p.getCurrentMajor(), "年级", p.getGradeYear() == null ? "-" : p.getGradeYear() + "年级");
        String gpaStr = p.getGpa() == null ? "-" : p.getGpa() + " / " + (p.getGpaScale() == null ? "?" : p.getGpaScale());
        addRow(t, "GPA", gpaStr, "英语", p.getEnglishLevel());
        addRow(t, "实习", flag(p.getHasInternship()), "科研", flag(p.getHasResearch()));
        addRow(t, "竞赛", flag(p.getHasCompetition()), "论文", flag(p.getHasPaper()));
        doc.add(t);
    }

    private String flag(Integer v) { return Integer.valueOf(1).equals(v) ? "有" : "无"; }

    private void addRow(PdfPTable t, String k1, String v1, String k2, String v2) {
        t.addCell(labelCell(k1));
        t.addCell(valueCell(v1 == null ? "-" : v1));
        t.addCell(labelCell(k2));
        t.addCell(valueCell(v2 == null ? "-" : v2));
    }

    // ── 三路径分析 ────────────────────────────────────────────────────────────

    private void renderAnalysis(Document doc, AnalysisResult analysis) throws DocumentException {
        doc.add(sectionTitle("📊 三路径分析"));

        if (analysis.getTopPath() != null) {
            Paragraph rec = new Paragraph();
            rec.add(new Chunk("推荐路径：", font(11, Font.BOLD, BRAND_DARK)));
            rec.add(new Chunk(pathName(analysis.getTopPath()), font(11, Font.BOLD, GREEN)));
            if (analysis.getTopPathReason() != null && !analysis.getTopPathReason().isBlank()) {
                rec.add(new Chunk("  " + analysis.getTopPathReason(), font(10, Font.NORMAL, Color.DARK_GRAY)));
            }
            rec.setSpacingBefore(4);
            rec.setSpacingAfter(10);
            doc.add(rec);
        } else {
            doc.add(new Paragraph("暂未形成明确推荐路径，请完善画像后重新分析。", font(10, Font.NORMAL, GRAY_TEXT)));
        }

        if (analysis.getPaths() != null) {
            for (PathScore ps : analysis.getPaths()) {
                if (ps != null) renderPathScore(doc, ps);
            }
        }
    }

    private void renderPathScore(Document doc, PathScore ps) throws DocumentException {
        Color scoreColor = ps.getOverall() >= 70 ? GREEN : ps.getOverall() >= 50 ? AMBER : RED;

        Paragraph head = new Paragraph();
        head.add(new Chunk(ps.getPathName() + "  ", font(12, Font.BOLD, BRAND_DARK)));
        head.add(new Chunk("综合得分 " + ps.getOverall(), font(12, Font.BOLD, scoreColor)));
        head.setSpacingBefore(8);
        head.setSpacingAfter(4);
        doc.add(head);

        // 维度得分表
        if (ps.getDimensions() != null && !ps.getDimensions().isEmpty()) {
            PdfPTable t = new PdfPTable(ps.getDimensions().size());
            t.setWidthPercentage(100);
            t.setSpacingAfter(4);
            for (PathScore.DimensionScore d : ps.getDimensions()) {
                PdfPCell cell = new PdfPCell();
                cell.setBackgroundColor(GRAY_LIGHT);
                cell.setBorderWidth(0);
                cell.setPadding(5);
                Paragraph cp = new Paragraph();
                cp.add(new Chunk(d.getName() + "\n", font(9, Font.NORMAL, GRAY_TEXT)));
                Color dc = d.getScore() >= 70 ? GREEN : d.getScore() >= 50 ? AMBER : RED;
                cp.add(new Chunk(String.valueOf(d.getScore()), font(13, Font.BOLD, dc)));
                cell.addElement(cp);
                t.addCell(cell);
            }
            doc.add(t);
        }

        // 建议
        if (ps.getAdvice() != null) {
            for (String a : ps.getAdvice()) {
                Paragraph ap = new Paragraph("·  " + a, font(9.5f, Font.NORMAL, Color.DARK_GRAY));
                ap.setIndentationLeft(8);
                ap.setSpacingBefore(2);
                doc.add(ap);
            }
        }
    }

    // ── 推荐路径规划 ──────────────────────────────────────────────────────────

    private void renderPlan(Document doc, PlanView plan) throws DocumentException {
        doc.add(divider());
        doc.add(sectionTitle("📅 " + plan.getPathName() + "备考规划"));

        if (plan.getTargetSummary() != null) {
            Paragraph p = new Paragraph();
            p.add(new Chunk("目标：", font(10.5f, Font.BOLD, BRAND_DARK)));
            p.add(new Chunk(plan.getTargetSummary(), font(10.5f, Font.NORMAL, Color.BLACK)));
            p.setSpacingBefore(6);
            p.setSpacingAfter(6);
            doc.add(p);
        }

        if (plan.getPhases() != null) {
            for (PlanView.PhaseGroup ph : plan.getPhases()) {
                PdfPTable phaseHeader = new PdfPTable(1);
                phaseHeader.setWidthPercentage(100);
                phaseHeader.setSpacingBefore(8);
                phaseHeader.setSpacingAfter(2);
                PdfPCell phCell = new PdfPCell(new Phrase(ph.getPhase(), font(11, Font.BOLD, BRAND_DARK)));
                phCell.setBackgroundColor(GRAY_LIGHT);
                phCell.setBorderWidth(0);
                phCell.setPadding(6);
                phCell.setPaddingLeft(10);
                phaseHeader.addCell(phCell);
                doc.add(phaseHeader);

                if (ph.getTasks() == null) continue;
                for (UserPlanTask task : ph.getTasks()) {
                    boolean done = Integer.valueOf(1).equals(task.getCompleted());
                    Paragraph tp = new Paragraph();
                    tp.setIndentationLeft(10);
                    tp.setSpacingBefore(3);
                    tp.add(new Chunk(done ? "[✓] " : "[  ] ", font(10, Font.NORMAL, done ? GRAY_TEXT : BRAND)));
                    tp.add(new Chunk(task.getTitle(), font(10, done ? Font.STRIKETHRU : Font.NORMAL,
                            done ? GRAY_TEXT : Color.BLACK)));
                    doc.add(tp);
                }
            }
        }
    }

    // ── 页脚 ──────────────────────────────────────────────────────────────────

    private void renderFooter(Document doc, ReportData data) throws DocumentException {
        doc.add(divider());
        String time = data.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Paragraph footer = new Paragraph("由 NextStep 智能决策平台生成  ·  " + time,
                font(8.5f, Font.ITALIC, GRAY_TEXT));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        doc.add(footer);
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, font(13, Font.BOLD, BRAND_DARK));
        p.setSpacingBefore(16);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(9.5f, Font.BOLD, GRAY_TEXT)));
        c.setBackgroundColor(GRAY_LIGHT);
        c.setBorderWidth(0);
        c.setPadding(5);
        return c;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(9.5f, Font.NORMAL, Color.BLACK)));
        c.setBorderWidth(0);
        c.setPadding(5);
        return c;
    }

    private PdfPTable divider() {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(0.6f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BRAND);
        cell.setBorderWidthBottom(0.6f);
        line.addCell(cell);
        return line;
    }

    private String pathName(String path) {
        return switch (path) {
            case "PG" -> "考研";
            case "CS" -> "考公";
            case "EM" -> "就业";
            default -> path;
        };
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
