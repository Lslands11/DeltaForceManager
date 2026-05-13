package com.deltaforce.manager.service.impl;

import com.deltaforce.manager.service.FileStorageService;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioFileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${game-monitor.minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' 已创建", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 初始化失败", e);
        }
    }

    @Override
    public String upload(Long accountId, String originalFilename, InputStream inputStream, long size, String contentType) {
        String ext = getExtension(originalFilename);
        String objectPath = "screenshots/" + accountId + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return objectPath;
        } catch (Exception e) {
            throw new RuntimeException("文件上传到 MinIO 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String objectPath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("从 MinIO 下载文件失败: " + e.getMessage(), e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
