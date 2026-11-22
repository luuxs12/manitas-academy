package com.mundomanitas.api.config;

import com.mundomanitas.api.service.AvatarStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Expone únicamente la carpeta de avatares, nunca el disco completo. */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    private final AvatarStorage storage;

    public UploadConfig(AvatarStorage storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(storage.directory().toUri().toString());
    }
}
