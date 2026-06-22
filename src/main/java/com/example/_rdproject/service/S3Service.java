package com.example._rdproject.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Value("${azure.storage.cdn-base-url}")
    private String cdnBaseUrl;

    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    public String upload(MultipartFile file) {
        try {
            // 파이썬 및 프론트 경로 규칙에 맞춰 'user-voice/' 프리픽스 부여
            String fileName = "user-voice/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            // Azure Blob Storage로 스트림 업로드 수행
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            // CDN 기반의 최종 접근 가능한 URL 주소 반환
            return cdnBaseUrl + "/audio-files/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Azure Blob 업로드 실패", e);
        }
    }
}