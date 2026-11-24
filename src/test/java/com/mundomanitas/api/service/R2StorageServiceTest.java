package com.mundomanitas.api.service;

import com.mundomanitas.api.config.R2Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2StorageServiceTest {

    @Mock
    private S3Client client;

    @Mock
    private S3Presigner presigner;

    private R2Properties properties;
    private R2StorageService service;

    @BeforeEach
    void setUp() {
        properties = new R2Properties();
        properties.setEndpoint("https://r2.example");
        properties.setBucket("public-bucket");
        properties.setPrivateBucket("private-bucket");
        properties.setAccessKey("key");
        properties.setSecretKey("secret");
        properties.setPublicBaseUrl("https://public.example");
        properties.setPresignSeconds(120);
        service = new R2StorageService(properties, client, presigner);
    }

    @Test
    @DisplayName("guarda los videos en el bucket privado y devuelve la clave, no una URL pública")
    void storesVideoInPrivateBucket() {
        var file = new MockMultipartFile("archivo", "clase.mp4", "video/mp4", new byte[]{1, 2, 3});

        String key = service.storeVideo(file);

        assertThat(key).matches("videos/[0-9a-f-]{36}\\.mp4");
        assertThat(key).doesNotContain("http");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("private-bucket");
        assertThat(captor.getValue().key()).isEqualTo(key);
    }

    @Test
    @DisplayName("guarda el subtítulo VTT en el bucket privado")
    void storesSubtitleInPrivateBucket() {
        var file = new MockMultipartFile("archivo", "clase.vtt", "text/vtt", new byte[]{1});

        String key = service.storeSubtitle(file);

        assertThat(key).matches("subtitles/[0-9a-f-]{36}\\.vtt");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("private-bucket");
    }

    @Test
    @DisplayName("firma una clave privada con el bucket y la duración configurada")
    void signsPrivateKey() {
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        URL signedUrl = mock(URL.class);
        when(signedUrl.toString()).thenReturn("https://firmado.example/clase?firma=1");
        when(signed.url()).thenReturn(signedUrl);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);

        String url = service.presignedUrl("videos/clase.mp4");

        assertThat(url).isEqualTo("https://firmado.example/clase?firma=1");
        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("private-bucket");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("videos/clase.mp4");
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    @DisplayName("rechaza claves que intentan salir de la carpeta del bucket")
    void rejectsTraversalKeys() {
        assertThatThrownBy(() -> service.presignedUrl("videos/../secreto.mp4"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(presigner);
    }

    @Test
    @DisplayName("no firma nada cuando el bucket privado no está configurado")
    void requiresPrivateConfiguration() {
        R2StorageService unconfigured =
                new R2StorageService(new R2Properties(), client, presigner);

        assertThatThrownBy(() -> unconfigured.presignedUrl("videos/clase.mp4"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(presigner);
    }

    @Test
    @DisplayName("borra del bucket privado solo claves válidas")
    void deletesPrivateKey() {
        service.deleteByKey("videos/clase.mp4");

        ArgumentCaptor<DeleteObjectRequest> captor =
                ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("private-bucket");
        assertThat(captor.getValue().key()).isEqualTo("videos/clase.mp4");
    }
}
