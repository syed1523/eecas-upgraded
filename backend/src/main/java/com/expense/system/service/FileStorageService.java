package com.expense.system.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface FileStorageService {
    String storeFile(MultipartFile file);

    Path loadFile(String fileName);
}
