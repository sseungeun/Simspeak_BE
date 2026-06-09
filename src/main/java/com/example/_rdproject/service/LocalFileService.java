package com.example._rdproject.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileService {
    // 프로젝트 루트 경로의 uploads 폴더
    private final String UPLOAD_DIR = "uploads/";

    public String upload(MultipartFile file) {
        try {
            // 폴더가 없으면 생성
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) directory.mkdirs();

            // 파일명 중복 방지를 위한 UUID 생성
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, file.getBytes());

            // 저장된 경로 반환 (나중에 브라우저에서 접근할 경로)
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}