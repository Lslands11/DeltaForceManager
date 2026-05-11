package com.deltaforce.manager.ocr;

import lombok.extern.slf4j.Slf4j;
import com.deltaforce.manager.entity.GameOcrConfig;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

@Component
@Slf4j
public class ImagePreprocessor {

    public BufferedImage preprocess(BufferedImage original, GameOcrConfig config) {
        BufferedImage image = crop(original, config.getCropX(), config.getCropY(),
                config.getCropWidth(), config.getCropHeight());
        image = scaleUp(image, config.getScaleFactor() != null ? config.getScaleFactor().doubleValue() : 2.0);
        image = toGrayscale(image);
        image = enhanceContrast(image);
        image = binarize(image, config.getThresholdValue() != null ? config.getThresholdValue() : 128);
        if (config.getInvertColors() != null && config.getInvertColors() == 1) {
            image = invertColors(image);
        }
        image = denoise(image);
        return image;
    }

    public BufferedImage crop(BufferedImage image, int x, int y, int width, int height) {
        int imgW = image.getWidth();
        int imgH = image.getHeight();
        int safeX = Math.max(0, Math.min(x, imgW - 1));
        int safeY = Math.max(0, Math.min(y, imgH - 1));
        int safeW = Math.min(width, imgW - safeX);
        int safeH = Math.min(height, imgH - safeY);
        if (safeW <= 0 || safeH <= 0) {
            log.warn("裁剪区域无效: x={}, y={}, w={}, h={}, imgW={}, imgH={}", x, y, width, height, imgW, imgH);
            return image;
        }
        return image.getSubimage(safeX, safeY, safeW, safeH);
    }

    public BufferedImage scaleUp(BufferedImage image, double factor) {
        if (factor <= 1.0) {
            return image;
        }
        int newWidth = (int) (image.getWidth() * factor);
        int newHeight = (int) (image.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }

    public BufferedImage toGrayscale(BufferedImage image) {
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = gray.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return gray;
    }

    public BufferedImage enhanceContrast(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int min = 255, max = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                if (pixel < min) min = pixel;
                if (pixel > max) max = pixel;
            }
        }

        if (max == min) {
            return image;
        }

        BufferedImage result = new BufferedImage(width, height, image.getType());
        double scale = 255.0 / (max - min);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                int stretched = (int) ((pixel - min) * scale);
                stretched = Math.max(0, Math.min(255, stretched));
                result.getRaster().setSample(x, y, 0, stretched);
            }
        }
        return result;
    }

    public BufferedImage binarize(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRaster().getSample(x, y, 0);
                int bw = pixel >= threshold ? 255 : 0;
                result.getRaster().setSample(x, y, 0, bw > 0 ? 1 : 0);
            }
        }
        return result;
    }

    public BufferedImage invertColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                result.setRGB(x, y, rgb ^ 0x00FFFFFF);
            }
        }
        return result;
    }

    public BufferedImage denoise(BufferedImage image) {
        float[] kernelData = {
                1f / 9f, 1f / 9f, 1f / 9f,
                1f / 9f, 1f / 9f, 1f / 9f,
                1f / 9f, 1f / 9f, 1f / 9f
        };
        BufferedImage src = image;
        if (image.getType() == BufferedImage.TYPE_BYTE_BINARY) {
            src = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = src.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        Kernel kernel = new Kernel(3, 3, kernelData);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }
}
