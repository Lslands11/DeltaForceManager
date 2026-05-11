package com.deltaforce.manager.ocr;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import com.deltaforce.manager.constant.GameMonitorConstants;
import com.deltaforce.manager.entity.GameOcrConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

@Component
@Slf4j
public class OcrPipeline {

    @Resource
    private ImagePreprocessor imagePreprocessor;
    @Resource
    private BalanceTextParser balanceTextParser;
    @Resource
    private Tesseract tesseract;

    public OcrResult execute(BufferedImage screenshot, GameOcrConfig ocrConfig) {
        OcrResult result = new OcrResult();

        BufferedImage processed = imagePreprocessor.preprocess(screenshot, ocrConfig);

        try {
            synchronized (tesseract) {
                int psm = ocrConfig.getTesseractPsm() != null ? ocrConfig.getTesseractPsm() : 7;
                tesseract.setPageSegMode(psm);
                tesseract.setTessVariable("tessedit_char_whitelist", GameMonitorConstants.TESSERACT_WHITELIST);
                String rawText = tesseract.doOCR(processed);
                result.setRawText(rawText != null ? rawText.trim() : "");
            }
        } catch (TesseractException e) {
            log.error("Tesseract OCR执行失败", e);
            throw new RuntimeException("OCR识别失败: " + e.getMessage(), e);
        }

        BalanceTextParser.ParseResult parseResult = balanceTextParser.parse(
                result.getRawText(), ocrConfig.getUnitSuffix());

        result.setAmount(parseResult.getAmount());
        result.setConfidence(parseResult.getConfidence());

        log.info("OCR结果: rawText={}, amount={}, confidence={}",
                result.getRawText(), result.getAmount(), result.getConfidence());
        return result;
    }

    @Data
    public static class OcrResult {
        private String rawText;
        private BigDecimal amount;
        private BigDecimal confidence;
    }
}
