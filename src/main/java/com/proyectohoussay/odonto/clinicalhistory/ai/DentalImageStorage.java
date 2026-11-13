package com.proyectohoussay.odonto.clinicalhistory.ai;

import org.springframework.web.multipart.MultipartFile;

public interface DentalImageStorage {
    Upload uploadForAnalysis(MultipartFile file);
    void deleteAfterAnalysis(String objectKey);

    record Upload(String objectKey, String presignedUrl) { }
}
