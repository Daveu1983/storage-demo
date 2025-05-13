package com.example.demo;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class StorageBucket {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    private final Storage storage;

    public StorageBucket() {
        try {
            this.storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(ServiceAccountCredentials.fromStream(
                            new FileInputStream(System.getenv("GCP_BUCKET_CRED"))))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load service account credentials", e);
        }
    }

    public String uploadFile(String fileName, byte[] content, String contentType) {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            throw new IllegalStateException("Bucket not found: " + bucketName);
        }
        Blob blob = bucket.create(fileName, content, contentType);
        return blob.getMediaLink();
    }

    public byte[] downloadFile(String fileName) {
        Blob blob = storage.get(bucketName, fileName);
        if (blob == null || !blob.exists()) {
            throw new IllegalStateException("File not found: " + fileName);
        }
        return blob.getContent();
    }

    @RestController
    @RequestMapping("/api/storage")
    public class StorageController {

        private final StorageBucket storageBucket;

        public StorageController(StorageBucket storageBucket) {
            this.storageBucket = storageBucket;
        }

        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
            try {
                String fileName = file.getOriginalFilename();
                byte[] content = file.getBytes();
                String contentType = file.getContentType();
                String mediaLink = storageBucket.uploadFile(fileName, content, contentType);
                return ResponseEntity.ok(mediaLink);
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
            }
        }

        @GetMapping("/download")
        public ResponseEntity<byte[]> downloadFile(@RequestParam("fileName") String fileName) {
            try {
                byte[] fileContent = storageBucket.downloadFile(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileContent);
            } catch (IllegalStateException e) {
                return ResponseEntity.status(404).body(null);
            }
        }
    }

}
