package com.proyectohoussay.odonto.clinicalhistory.ai;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "true")
public class S3DentalImageStorage implements DentalImageStorage {
    private final String bucket;
    private final long maxSize;
    private final Duration urlTtl;
    private final S3Client client;
    private final S3Presigner presigner;

    public S3DentalImageStorage(
            @Value("${storage.s3.endpoint:}") String endpoint,
            @Value("${storage.s3.region:us-east-1}") String region,
            @Value("${storage.s3.bucket}") String bucket,
            @Value("${storage.s3.access-key}") String accessKey,
            @Value("${storage.s3.secret-key}") String secretKey,
            @Value("${storage.s3.path-style-access:false}") boolean pathStyle,
            @Value("${storage.s3.presigned-url-ttl-seconds:900}") long ttlSeconds,
            @Value("${storage.s3.max-object-size-bytes:26214400}") long maxSize) {
        if (bucket.isBlank() || accessKey.isBlank() || secretKey.isBlank()) {
            throw new IllegalStateException("S3 está habilitado, pero faltan S3_BUCKET, S3_ACCESS_KEY o S3_SECRET_KEY.");
        }
        if (ttlSeconds < 60 || ttlSeconds > 3600) {
            throw new IllegalStateException("S3_PRESIGNED_URL_TTL_SECONDS debe estar entre 60 y 3600 segundos.");
        }
        this.bucket = bucket;
        this.maxSize = maxSize;
        this.urlTtl = Duration.ofSeconds(ttlSeconds);
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var s3Config = S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build();
        var clientBuilder = S3Client.builder().region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(s3Config);
        var presignerBuilder = S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(s3Config);
        if (endpoint != null && !endpoint.isBlank()) {
            URI endpointUri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(endpointUri.getScheme()) || endpointUri.getHost() == null) {
                throw new IllegalStateException("S3_ENDPOINT debe ser una URL HTTPS accesible desde internet para DR. ANNIE.");
            }
            clientBuilder.endpointOverride(endpointUri);
            presignerBuilder.endpointOverride(endpointUri);
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @Override
    public Upload uploadForAnalysis(MultipartFile file) {
        if (file.getSize() > maxSize) throw new IllegalArgumentException("El archivo supera el máximo permitido para S3.");
        String type = file.getContentType() == null || file.getContentType().isBlank()
                ? (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().matches(".*\\.(dcm|dicom)$")
                    ? "application/dicom" : "application/octet-stream")
                : file.getContentType();
        String extension = extension(type);
        String key = "radiografias-temporales/" + UUID.randomUUID() + extension;
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            var getRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder().bucket(bucket).key(key).build();
            String url = presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(urlTtl).getObjectRequest(getRequest).build())
                    .url().toExternalForm();
            return new Upload(key, url);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la radiografía para cargarla al almacenamiento privado.", e);
        } catch (RuntimeException e) {
            try { client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()); } catch (RuntimeException ignored) { }
            throw new IllegalStateException("No se pudo cargar la radiografía al almacenamiento privado S3.", e);
        }
    }

    @Override
    public void deleteAfterAnalysis(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private static String extension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/dicom", "application/octet-stream" -> ".dcm";
            default -> throw new IllegalArgumentException("Formato de radiografía no admitido.");
        };
    }
}
