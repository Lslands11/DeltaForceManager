package com.deltaforce.manager.ocr;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class BalanceTextParser {

    private static final Map<String, BigDecimal> UNIT_MULTIPLIERS = new HashMap<>();
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([\\d,]+)\\s*([万WwKkMmBb]?)");

    static {
        UNIT_MULTIPLIERS.put("万", new BigDecimal("10000"));
        UNIT_MULTIPLIERS.put("W", new BigDecimal("10000"));
        UNIT_MULTIPLIERS.put("w", new BigDecimal("10000"));
        UNIT_MULTIPLIERS.put("K", new BigDecimal("1000"));
        UNIT_MULTIPLIERS.put("k", new BigDecimal("1000"));
        UNIT_MULTIPLIERS.put("M", new BigDecimal("1000000"));
        UNIT_MULTIPLIERS.put("m", new BigDecimal("1000000"));
        UNIT_MULTIPLIERS.put("B", new BigDecimal("1000000000"));
        UNIT_MULTIPLIERS.put("b", new BigDecimal("1000000000"));
    }

    public ParseResult parse(String rawText, String expectedUnitSuffix) {
        ParseResult result = new ParseResult();
        if (rawText == null || rawText.trim().isEmpty()) {
            result.setConfidence(BigDecimal.ZERO);
            return result;
        }

        String cleaned = cleanText(rawText);
        result.setCleanedText(cleaned);

        Matcher matcher = NUMBER_PATTERN.matcher(cleaned);
        if (!matcher.find()) {
            result.setConfidence(BigDecimal.ZERO);
            return result;
        }

        String numberPart = matcher.group(1).replace(",", "");
        String unitPart = matcher.group(2);

        try {
            BigDecimal value = new BigDecimal(numberPart);
            BigDecimal multiplier = BigDecimal.ONE;

            if (unitPart != null && !unitPart.isEmpty()) {
                multiplier = UNIT_MULTIPLIERS.getOrDefault(unitPart, BigDecimal.ONE);
                result.setHasUnitSuffix(true);
            }

            BigDecimal amount = value.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            result.setAmount(amount);

            result.setConfidence(computeConfidence(rawText, cleaned, unitPart, expectedUnitSuffix));
        } catch (NumberFormatException e) {
            log.warn("金额解析失败: raw={}, cleaned={}", rawText, cleaned);
            result.setConfidence(BigDecimal.ZERO);
        }

        return result;
    }

    private String cleanText(String text) {
        String cleaned = text.trim();
        cleaned = cleaned.replace("O", "0")
                .replace("o", "0")
                .replace("l", "1")
                .replace("I", "1")
                .replace("|", "1")
                .replace("S", "5")
                .replace("s", "5")
                .replace(" ", "");
        cleaned = cleaned.replaceAll("[^\\d.,万WwKkMmBb]", "");

        // 游戏币为整数，OCR误识别的小数点全部视为千位分隔符
        cleaned = cleaned.replace(".", "");
        return cleaned;
    }

    private BigDecimal computeConfidence(String rawText, String cleanedText, String detectedUnit, String expectedUnit) {
        double confidence = 50.0;

        double similarity = 1.0 - (double) Math.abs(rawText.length() - cleanedText.length()) / Math.max(rawText.length(), 1);
        confidence += similarity * 20;

        if (cleanedText.matches(".*\\d+.*")) {
            confidence += 15;
        }

        // 单位匹配加分（仅在配置了预期单位时才校验）
        if (expectedUnit != null && !expectedUnit.isEmpty()) {
            if (detectedUnit != null && !detectedUnit.isEmpty()
                    && UNIT_MULTIPLIERS.containsKey(detectedUnit)
                    && UNIT_MULTIPLIERS.containsKey(expectedUnit)
                    && UNIT_MULTIPLIERS.get(detectedUnit).equals(UNIT_MULTIPLIERS.get(expectedUnit))) {
                confidence += 15;
            }
        } else {
            // 未配置预期单位时，识别到数字就加分
            confidence += 10;
        }

        confidence = Math.max(0, Math.min(100, confidence));
        return new BigDecimal(confidence).setScale(2, RoundingMode.HALF_UP);
    }

    @Data
    public static class ParseResult {
        private BigDecimal amount;
        private BigDecimal confidence;
        private String cleanedText;
        private boolean hasUnitSuffix;
    }
}
