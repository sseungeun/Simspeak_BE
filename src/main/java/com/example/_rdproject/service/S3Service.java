
package com.example._rdproject.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class S3Service {
    public String upload(MultipartFile file) {
        // 실제 S3 업로드 로직(AmazonS3 클라이언트 사용 등) 구현부
        return "https://your-s3-bucket-url/" + file.getOriginalFilename();
    }
}
