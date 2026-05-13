package com.deltaforce.manager.service;

import java.io.InputStream;

public interface FileStorageService {

    /**
     * 上传文件到对象存储
     *
     * @return 对象路径，如 screenshots/1/abc123.png
     */
    String upload(Long accountId, String originalFilename, InputStream inputStream, long size, String contentType);

    /**
     * 根据对象路径获取文件输入流
     */
    InputStream download(String objectPath);
}
