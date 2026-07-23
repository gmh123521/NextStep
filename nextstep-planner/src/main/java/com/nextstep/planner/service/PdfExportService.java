package com.nextstep.planner.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nextstep.common.exception.BizException;
import com.nextstep.planner.dto.PlanView;
import com.nextstep.planner.entity.UserPlanTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 把 PlanView 渲染成 PDF 字节流。
 * OpenPDF 内置 CJK 编码（STSong-Light + UniGB-UCS2-H），不需要嵌入字体文件，部署轻量。
 */
@Slf4j
@Service
public class PdfExportService {

    /** 主题色（与前端 Element Plus 保持一致） */
    private static final Color BRAND      = new Color(64, 158, 255);
    private static final Color BRAND_DARK = new Color(48, 122, 197);
    private static final Color GRAY_LIGHT = new Color(243, 244, 246);
    private static final Color GRAY_TEXT  = new Color(107, 114, 128);
    private static final Color RISK_BG    = new Color(254, 243, 199);
    private static final Color RISK_TEXT  = new Color(146, 64, 14);

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

    /** 把 PlanView 渲染成 PDF 字节数组 */
    public byte[] render(PlanView plan) {
        if (plan == null) throw new BizException("规划不存在，无法导出");

        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            // 显式 PDF 1.4 + 传统 xref 表：浏览器内置 PDF 阅读器（Edge/Chrome/Safari）对 1.4 兼容性最好
            // 不调用 setFullCompression()，避免被升级成 PDF 1.5 + xref stream
            writer.setPdfVersion(PdfWriter.VERSION_1_4);
            doc.open();

            renderHeader(doc, plan);
            renderSummary(doc, plan);
            renderStrategy(doc, plan);
            renderRiskAlerts(doc, plan);
            renderPhases(doc, plan);
            renderFooter(doc);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            log.error("[pdf] 渲染失败: {}", e.getMessage(), e);
            throw new BizException("PDF 生成失败：" + e.getMessage());
        }
    }

    private void renderHeader(Document doc, PlanView plan) throws DocumentException {
        Paragraph brand = new Paragraph("NextStep", font(11, Font.BOLD, BRAND));
        brand.setAlignment(Element.ALIGN_LEFT);
        doc.add(brand);

        Paragraph title = new Paragraph("我的" + plan.getPathName() + "规划", font(22, Font.BOLD, BRAND_DARK));
        title.setSpacingBefore(6);
        title.setSpacingAfter(6);
        doc.add(title);

        Paragraph subtitle = new Paragraph(
                "战线 " + plan.getTotalMonths() + " 个月  ·  共 " + plan.getTotalTasks() + " 个任务  ·  已完成 " + plan.getProgressPct() + "%",
                font(10, Font.NORMAL, GRAY_TEXT));
        subtitle.setSpacingAfter(14);
        doc.add(subtitle);

        // 分隔线
        doc.add(divider());
    }

    private void renderSummary(Document doc, PlanView plan) throws DocumentException {
        if (plan.getTargetSummary() == null || plan.getTargetSummary().isBlank()) return;
        Paragraph p = new Paragraph();
        p.add(new Chunk("🎯 目标   ", font(12, Font.BOLD, BRAND_DARK)));
        p.add(new Chunk(plan.getTargetSummary(), font(11, Font.NORMAL, Color.BLACK)));
        p.setSpacingBefore(12);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private void renderStrategy(Document doc, PlanView plan) throws DocumentException {
        if (plan.getStrategy() == null || plan.getStrategy().isBlank()) return;
        Paragraph head = new Paragraph("📝 总体策略", font(12, Font.BOLD, BRAND_DARK));
        head.setSpacingBefore(10);
        head.setSpacingAfter(4);
        doc.add(head);

        Paragraph body = new Paragraph(plan.getStrategy(), font(10.5f, Font.NORMAL, Color.DARK_GRAY));
        body.setLeading(16f);
        body.setSpacingAfter(12);
        doc.add(body);
    }

    private void renderRiskAlerts(Document doc, PlanView plan) throws DocumentException {
        List<String> risks = plan.getRiskAlerts();
        if (risks == null || risks.isEmpty()) return;
        Paragraph head = new Paragraph("⚠ 风险预警", font(12, Font.BOLD, RISK_TEXT));
        head.setSpacingBefore(8);
        head.setSpacingAfter(4);
        doc.add(head);

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        t.setSpacingAfter(14);
        for (String r : risks) {
            PdfPCell cell = new PdfPCell(new Phrase("·  " + r, font(10, Font.NORMAL, RISK_TEXT)));
            cell.setBackgroundColor(RISK_BG);
            cell.setBorderWidth(0);
            cell.setPadding(8);
            cell.setPaddingLeft(12);
            t.addCell(cell);
        }
        doc.add(t);
    }

    private void renderPhases(Document doc, PlanView plan) throws DocumentException {
        if (plan.getPhases() == null || plan.getPhases().isEmpty()) return;

        Paragraph head = new Paragraph("📅 任务清单", font(12, Font.BOLD, BRAND_DARK));
        head.setSpacingBefore(6);
        head.setSpacingAfter(6);
        doc.add(head);

        for (PlanView.PhaseGroup ph : plan.getPhases()) {
            // 阶段标签：浅蓝色背景条
            PdfPTable phaseHeader = new PdfPTable(1);
            phaseHeader.setWidthPercentage(100);
            phaseHeader.setSpacingBefore(8);
            phaseHeader.setSpacingAfter(2);
            PdfPCell phCell = new PdfPCell(new Phrase(ph.getPhase(), font(11.5f, Font.BOLD, BRAND_DARK)));
            phCell.setBackgroundColor(GRAY_LIGHT);
            phCell.setBorderWidth(0);
            phCell.setPadding(6);
            phCell.setPaddingLeft(10);
            phaseHeader.addCell(phCell);
            doc.add(phaseHeader);

            for (UserPlanTask task : ph.getTasks()) {
                doc.add(renderTask(task));
            }
        }
    }

    private Paragraph renderTask(UserPlanTask task) {
        Paragraph p = new Paragraph();
        p.setIndentationLeft(10);
        p.setSpacingBefore(4);
        p.setSpacingAfter(2);

        boolean done = Integer.valueOf(1).equals(task.getCompleted());
        String box = done ? "[✓] " : "[  ] ";
        Color titleColor = done ? GRAY_TEXT : Color.BLACK;
        int titleStyle = done ? Font.STRIKETHRU : Font.BOLD;

        p.add(new Chunk(box, font(10.5f, Font.NORMAL, done ? GRAY_TEXT : BRAND)));
        if (task.getSubject() != null && !task.getSubject().isBlank()) {
            p.add(new Chunk(task.getSubject() + "  ", font(9.5f, Font.BOLD, BRAND)));
        }
        p.add(new Chunk(task.getTitle(), font(10.5f, titleStyle, titleColor)));

        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            p.add(Chunk.NEWLINE);
            Chunk desc = new Chunk("       " + task.getDescription(),
                    font(9.5f, Font.NORMAL, GRAY_TEXT));
            p.add(desc);
        }
        return p;
    }

    private void renderFooter(Document doc) throws DocumentException {
        doc.add(divider());
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Paragraph footer = new Paragraph(
                "由 NextStep 智能规划生成  ·  " + time,
                font(8.5f, Font.ITALIC, GRAY_TEXT));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        doc.add(footer);
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
}
