package com.mundomanitas.api.service;

import com.mundomanitas.api.config.R2Properties;
import org.springframework.beans.factory.annotation.Autowired;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Almacenamiento en Cloudflare R2: imágenes públicas y videos privados.
 *
 * <p>R2 expone una API compatible con S3. Los videos y sus subtítulos van a un
 * bucket sin acceso público y solo se entregan con URLs prefirmadas de corta
 * duración. Sin credenciales el servicio queda desactivado y nunca finge que
 * un archivo fue subido.</p>
 */
@Service
public class R2StorageService implements AutoCloseable {

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

    public static final Map<String, String> SUBTITLE_EXTENSIONS = Map.of("text/vtt", "vtt");

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_./-]+");

    private final R2Properties properties;
    private final S3Client client;
    private final S3Presigner presigner;

    @Autowired
    public R2StorageService(R2Properties properties) {
        this(properties,
                properties.hasCredentials() ? buildClient(properties) : null,
                properties.isVideosEnabled() ? buildPresigner(properties) : null);
    }

    /** Constructor de pruebas: inyecta el cliente y el firmante sin red. */
    R2StorageService(R2Properties properties, S3Client client, S3Presigner presigner) {
        this.properties = properties;
        this.client = client;
        this.presigner = presigner;
    }

    public boolean enabled() {
        return properties.isEnabled() && client != null;
    }

    public boolean videosEnabled() {
        return properties.isVideosEnabled() && client != null && presigner != null;
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

    /** Sube el video al bucket privado y devuelve su clave, nunca una URL. */
    public String storeVideo(MultipartFile file) {
        requirePrivateStorage();
        return storeKey(file, "videos", VIDEO_EXTENSIONS,
                properties.getMaxVideoBytes(), properties.getPrivateBucket());
    }

    /** Sube un único subtítulo VTT en español al bucket privado. */
    public String storeSubtitle(MultipartFile file) {
        requirePrivateStorage();
        return storeKey(file, "subtitles", SUBTITLE_EXTENSIONS,
                properties.getMaxSubtitleBytes(), properties.getPrivateBucket());
    }

    /** URL prefirmada de lectura, válida solo unos minutos. */
    public String presignedUrl(String key) {
        requirePrivateStorage();
        requireValidKey(key);
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.getPresignSeconds()))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.getPrivateBucket())
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    /** Borra un objeto del bucket privado cuando la clave es válida. */
    public void deleteByKey(String key) {
        if (!videosEnabled() || key == null) return;
        requireValidKey(key);
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getPrivateBucket())
                .key(key)
                .build());
    }

    public String store(MultipartFile file, String folder,
                        Map<String, String> extensions, long maxBytes) {
        requireConfigured();
        String key = storeKey(file, folder, extensions, maxBytes, properties.getBucket());
        return url(key);
    }

    /** Borra el objeto remoto cuando la URL guardada apunta al bucket público. */
    public void deleteByUrl(String url) {
        if (!enabled() || url == null) return;
        String base = properties.getPublicBaseUrl().replaceAll("/+$", "") + "/";
        if (!url.startsWith(base)) return;
        String key = url.substring(base.length());
        if (!KEY_PATTERN.matcher(key).matches() || key.contains("..")) return;
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }

    private String storeKey(MultipartFile file, String folder,
                            Map<String, String> extensions, long maxBytes, String bucket) {
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
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();
        try (InputStream input = file.getInputStream()) {
            client.putObject(request, RequestBody.fromInputStream(input, file.getSize()));
        } catch (IOException | S3Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "No se pudo guardar el archivo en R2", exception);
        }
        return key;
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

    private void requirePrivateStorage() {
        if (!videosEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El almacenamiento privado de videos no está configurado en este entorno");
        }
    }

    private static void requireValidKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches() || key.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clave de video inválida");
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

    private static S3Presigner buildPresigner(R2Properties properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }
}
