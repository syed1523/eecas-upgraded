package com.expense.system.service.impl;

import com.expense.system.exception.InvalidFileException;
import com.expense.system.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFileName.contains("..")) {
            throw new InvalidFileException("Filename contains invalid path sequence: " + originalFileName);
        }

        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        } catch (Exception e) {
            throw new InvalidFileException("File extension is missing or invalid.");
        }

        List<String> allowedExtensions = Arrays.asList(".pdf", ".png", ".jpg", ".jpeg");
        if (!allowedExtensions.contains(fileExtension)) {
            throw new InvalidFileException("Only PDF, PNG, and JPG files are allowed. Rejected: " + fileExtension);
        }

        String contentType = file.getContentType();
        List<String> allowedContentTypes = Arrays.asList("application/pdf", "image/png", "image/jpeg");
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new InvalidFileException("Invalid or disallowed MIME type: " + contentType
                    + ". Allowed: application/pdf, image/png, image/jpeg");
        }

        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new InvalidFileException("File size " + (file.getSize() / 1024 / 1024) + "MB exceeds the 5MB limit.");
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public Path loadFile(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }
}
