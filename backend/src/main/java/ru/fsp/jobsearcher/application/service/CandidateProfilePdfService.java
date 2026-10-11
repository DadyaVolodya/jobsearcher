package ru.fsp.jobsearcher.application.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;

@Service
@RequiredArgsConstructor
public class CandidateProfilePdfService {

    private final CandidateService candidateService;

    @Transactional(readOnly = true)
    public byte[] exportMine(CurrentUser user) {
        CandidateProfile p = candidateService.requireMine(user);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float y = 780;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                y = write(cs, titleFont, 16, 50, y, "FSP Jobsearcher - Candidate profile");
                y -= 12;
                y = write(cs, bodyFont, 11, 50, y, "Name: " + safe(p.getFullName()));
                y = write(cs, bodyFont, 11, 50, y, "City: " + safe(p.getCity()));
                y = write(cs, bodyFont, 11, 50, y, "Phone: " + safe(p.getPhone()));
                y = write(cs, bodyFont, 11, 50, y, "Industry: " + safe(p.getIndustryCode())
                        + " / " + safe(p.getSpecializationCode()));
                y = write(cs, bodyFont, 11, 50, y, "Claimed grade: " + str(p.getClaimedGrade()));
                y = write(cs, bodyFont, 11, 50, y, "Assigned grade: " + str(p.getAssignedGrade())
                        + (p.isGradeConfirmed() ? " (confirmed)" : " (not confirmed)"));
                y = write(cs, bodyFont, 11, 50, y, "Category: " + safe(p.getCategoryCode()));
                y = write(cs, bodyFont, 11, 50, y, "Test score: " + String.format("%.2f", p.getTestScore()));
                y = write(cs, bodyFont, 11, 50, y, "Profile score: " + String.format("%.2f", p.getProfileScore()));
                y = write(cs, bodyFont, 11, 50, y, "Stack: " + join(p.getStack()));
                y = write(cs, bodyFont, 11, 50, y, "Soft skills: " + join(p.getSoftSkills()));
                y = write(cs, bodyFont, 11, 50, y, "FSP id: " + safe(p.getFspParticipantId()));
                y = write(cs, bodyFont, 11, 50, y, "FSP grade: " + str(p.getFspGrade()));
                y -= 8;
                y = write(cs, titleFont, 12, 50, y, "About");
                y = writeWrapped(cs, bodyFont, 10, 50, y, safe(p.getAbout()), 90);
                y -= 8;
                y = write(cs, titleFont, 12, 50, y, "Resume");
                writeWrapped(cs, bodyFont, 10, 50, y, safe(p.getResumeText()), 90);
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сформировать PDF профиля", e);
        }
    }

    private static float write(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(text));
        cs.endText();
        return y - (size + 6);
    }

    private static float writeWrapped(
            PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text, int width
    ) throws IOException {
        String[] words = ascii(text).split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > width) {
                y = write(cs, font, size, x, y, line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            y = write(cs, font, size, x, y, line.toString());
        }
        return y;
    }

    private static String join(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return values.stream().map(CandidateProfilePdfService::safe).collect(Collectors.joining(", "));
    }

    private static String str(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** Helvetica не содержит кириллицу - транслитерируем для стабильного PDF в MVP. */
    private static String ascii(String input) {
        if (input == null) {
            return "-";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            sb.append(switch (c) {
                case 'а', 'А' -> 'a';
                case 'б', 'Б' -> 'b';
                case 'в', 'В' -> 'v';
                case 'г', 'Г' -> 'g';
                case 'д', 'Д' -> 'd';
                case 'е', 'Е', 'ё', 'Ё' -> 'e';
                case 'ж', 'Ж' -> "zh";
                case 'з', 'З' -> 'z';
                case 'и', 'И' -> 'i';
                case 'й', 'Й' -> 'y';
                case 'к', 'К' -> 'k';
                case 'л', 'Л' -> 'l';
                case 'м', 'М' -> 'm';
                case 'н', 'Н' -> 'n';
                case 'о', 'О' -> 'o';
                case 'п', 'П' -> 'p';
                case 'р', 'Р' -> 'r';
                case 'с', 'С' -> 's';
                case 'т', 'Т' -> 't';
                case 'у', 'У' -> 'u';
                case 'ф', 'Ф' -> 'f';
                case 'х', 'Х' -> 'h';
                case 'ц', 'Ц' -> "ts";
                case 'ч', 'Ч' -> "ch";
                case 'ш', 'Ш' -> "sh";
                case 'щ', 'Щ' -> "sch";
                case 'ъ', 'Ъ', 'ь', 'Ь' -> "";
                case 'ы', 'Ы' -> 'y';
                case 'э', 'Э' -> 'e';
                case 'ю', 'Ю' -> "yu";
                case 'я', 'Я' -> "ya";
                default -> (c < 32 || c > 126) ? '?' : c;
            });
        }
        return sb.toString();
    }
}
