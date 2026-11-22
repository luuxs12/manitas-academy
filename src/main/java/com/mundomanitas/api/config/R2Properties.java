package com.mundomanitas.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de Cloudflare R2. El bucket y las claves llegan por entorno;
 * sin ellos el almacenamiento remoto queda apagado y la interfaz lo indica.
 */
@Component
@ConfigurationProperties(prefix = "app.r2")
public class R2Properties {

    private String endpoint = "";
    private String bucket = "";
    private String accessKey = "";
    private String secretKey = "";
    private String publicBaseUrl = "";
    private String region = "auto";
    private long maxImageBytes = 5L * 1024 * 1024;
    private long maxVideoBytes = 500L * 1024 * 1024;

    public boolean isEnabled() {
        return !endpoint.isBlank() && !bucket.isBlank()
                && !accessKey.isBlank() && !secretKey.isBlank()
                && !publicBaseUrl.isBlank();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    public void setMaxImageBytes(long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }

    public long getMaxVideoBytes() {
        return maxVideoBytes;
    }

    public void setMaxVideoBytes(long maxVideoBytes) {
        this.maxVideoBytes = maxVideoBytes;
    }
}
