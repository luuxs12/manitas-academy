package com.mundomanitas.api.config;

import com.mundomanitas.api.service.AvatarStorage;
import com.mundomanitas.api.service.CursoPortadaStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Expone solo las carpetas de imágenes, nunca el disco completo. */
@Configuration
public class CargaConfig implements WebMvcConfigurer {

    private final AvatarStorage avatars;
    private final CursoPortadaStorage covers;

    public CargaConfig(AvatarStorage avatars, CursoPortadaStorage covers) {
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
