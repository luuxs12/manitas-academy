package com.mundomanitas.api.service;

import com.mundomanitas.api.config.R2Properties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Almacenamiento de avatares, imágenes y videos en Cloudflare R2.
 *
 * <p>R2 expone una API compatible con S3; por eso se usa el SDK oficial con
 * ruta de estilo path. Si faltan credenciales el servicio queda desactivado
 * y nunca finge que un archivo fue subido.</p>
 */
@Service
public class R2StorageService {

    public static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    public static final Map<String, String> VIDEO_EXTENSIONS = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov"
    );

    private final R2Properties properties;
    private final S3Client client;

    public R2StorageService(R2Properties properties) {
        this.properties = properties;
        this.client = properties.isEnabled() ? buildClient(properties) : null;
    }

    public boolean enabled() {
        return client != null;
    }

    public String publicBaseUrl() {
        return properties.getPublicBaseUrl();
    }

    public String storeAvatar(MultipartFile file, long maxBytes) {
        return store(file, "avatars", IMAGE_EXTENSIONS, maxBytes);
    }

    public String storeImage(MultipartFile file) {
        return store(file, "assets", IMAGE_EXTENSIONS, properties.getMaxImageBytes());
    }

    public String storeVideo(MultipartFile file) {
        return store(file, "videos", VIDEO_EXTENSIONS, properties.getMaxVideoBytes());
    }

    public String store(MultipartFile file, String folder,
                        Map<String, String> extensions, long maxBytes) {
        requireConfigured();
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí un archivo");
        }
        String extension = extensions.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El formato del archivo no está permitido");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El archivo supera el tamaño máximo permitido");
        }

        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();
        try (InputStream input = file.getInputStream()) {
            client.putObject(request, RequestBody.fromInputStream(input, file.getSize()));
        } catch (IOException | S3Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "No se pudo guardar el archivo en R2", exception);
        }
        return url(key);
    }

    /** Borra el objeto remoto cuando la URL guardada apunta a este bucket. */
    public void deleteByUrl(String url) {
        if (!enabled() || url == null) return;
        String base = properties.getPublicBaseUrl().replaceAll("/+$", "") + "/";
        if (!url.startsWith(base)) return;
        String key = url.substring(base.length());
        if (!key.matches("[A-Za-z0-9_./-]+")) return;
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }

    private String url(String key) {
        return properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + key;
    }

    private void requireConfigured() {
        if (!enabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El almacenamiento R2 no está configurado en este entorno");
        }
    }

    private static S3Client buildClient(R2Properties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }
}
