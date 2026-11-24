package com.mundomanitas.api.config;

import com.mundomanitas.api.service.AvatarStorage;
import com.mundomanitas.api.service.CourseCoverStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Expone solo las carpetas de imágenes, nunca el disco completo. */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    private final AvatarStorage avatars;
    private final CourseCoverStorage covers;

    public UploadConfig(AvatarStorage avatars, CourseCoverStorage covers) {
        this.avatars = avatars;
        this.covers = covers;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(avatars.directory().toUri().toString());
        registry.addResourceHandler("/uploads/covers/**")
                .addResourceLocations(covers.directory().toUri().toString());
    }
}
