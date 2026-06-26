package com.schoolmgmt.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.schoolmgmt.report.exception.BackendApiException;
import com.schoolmgmt.report.model.Student;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class StudentPdfService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(33, 37, 41));
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(73, 80, 87));
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Color HEADER_BG = new Color(52, 58, 64);

    public byte[] generate(Student student) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("Student Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("School Management System", VALUE_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(18);
            document.add(subtitle);

            document.add(section("Personal Information"));
            document.add(detailTable(new String[][]{
                    {"Name", student.getName()},
                    {"Email", student.getEmail()},
                    {"Phone", student.getPhone()},
                    {"Gender", student.getGender()},
                    {"Date of Birth", dateOnly(student.getDob())},
                    {"System Access", student.getSystemAccess() == null ? null : (student.getSystemAccess() ? "Enabled" : "Disabled")},
            }));

            document.add(section("Academic Information"));
            document.add(detailTable(new String[][]{
                    {"Class", student.getClassName()},
                    {"Section", student.getSection()},
                    {"Roll", student.getRoll() == null ? null : String.valueOf(student.getRoll())},
                    {"Admission Date", dateOnly(student.getAdmissionDate())},
                    {"Reporter", student.getReporterName()},
            }));

            document.add(section("Parents & Guardian"));
            document.add(detailTable(new String[][]{
                    {"Father Name", student.getFatherName()},
                    {"Father Phone", student.getFatherPhone()},
                    {"Mother Name", student.getMotherName()},
                    {"Mother Phone", student.getMotherPhone()},
                    {"Guardian Name", student.getGuardianName()},
                    {"Guardian Phone", student.getGuardianPhone()},
                    {"Relation", student.getRelationOfGuardian()},
            }));

            document.add(section("Address"));
            document.add(detailTable(new String[][]{
                    {"Current Address", student.getCurrentAddress()},
                    {"Permanent Address", student.getPermanentAddress()},
            }));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BackendApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate the PDF report");
        }
    }

    private String dateOnly(String value) {
        if (value != null && value.length() >= 10 && value.charAt(4) == '-') {
            return value.substring(0, 10);
        }
        return value;
    }

    private Paragraph section(String label) {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(label, SECTION_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorder(0);
        bar.addCell(cell);

        Paragraph wrapper = new Paragraph();
        wrapper.setSpacingBefore(12);
        wrapper.setSpacingAfter(6);
        wrapper.add(bar);
        return wrapper;
    }

    private PdfPTable detailTable(String[][] rows) {
        PdfPTable table = new PdfPTable(new float[]{1f, 2f});
        table.setWidthPercentage(100);
        for (String[] row : rows) {
            table.addCell(labelCell(row[0]));
            table.addCell(valueCell(row[1]));
        }
        return table;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL_FONT));
        cell.setBackgroundColor(new Color(248, 249, 250));
        cell.setPadding(6);
        cell.setBorderColor(new Color(222, 226, 230));
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null || text.isBlank() ? "-" : text, VALUE_FONT));
        cell.setPadding(6);
        cell.setBorderColor(new Color(222, 226, 230));
        return cell;
    }
}
