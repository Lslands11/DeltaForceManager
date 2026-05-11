package com.deltaforce.manager.constant;

public final class GameMonitorConstants {

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    public static final int OCR_STATUS_PENDING = 0;
    public static final int OCR_STATUS_SUCCESS = 1;
    public static final int OCR_STATUS_FAILED = 2;
    public static final int OCR_STATUS_REVIEW = 3;

    public static final int SOURCE_OCR = 1;
    public static final int SOURCE_MANUAL = 2;
    public static final int SOURCE_CORRECTED = 3;

    public static final double CONFIDENCE_THRESHOLD = 80.0;

    public static final String TESSERACT_WHITELIST = "0123456789.,KkMm万Ww";

    private GameMonitorConstants() {
    }
}
