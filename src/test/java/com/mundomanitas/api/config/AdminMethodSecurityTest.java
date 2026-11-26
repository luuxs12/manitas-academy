package com.mundomanitas.api.config;

import com.mundomanitas.api.controller.admin.AdminAuditoriaController;
import com.mundomanitas.api.controller.admin.AdminCertificadoController;
import com.mundomanitas.api.controller.admin.AdminContenidoController;
import com.mundomanitas.api.controller.admin.AdminController;
import com.mundomanitas.api.controller.admin.AdminCursoController;
import com.mundomanitas.api.controller.admin.AdminExportacionController;
import com.mundomanitas.api.controller.admin.AdminMonitoreoApiController;
import com.mundomanitas.api.controller.admin.AdminMonitoreoController;
import com.mundomanitas.api.controller.admin.AdminNotificacionApiController;
import com.mundomanitas.api.controller.admin.AdminCuestionarioController;
import com.mundomanitas.api.controller.admin.AdminReporteController;
import com.mundomanitas.api.controller.admin.AdminSearchController;
import com.mundomanitas.api.controller.admin.AdminSesionPdfController;
import com.mundomanitas.api.controller.admin.AdminUsuarioController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Seguridad por método del panel administrativo")
class AdminMethodSecurityTest {

    private static final List<Class<?>> ADMIN_CONTROLLERS = List.of(
            AdminController.class,
            AdminUsuarioController.class,
            AdminCursoController.class,
            AdminContenidoController.class,
            AdminCertificadoController.class,
            AdminAuditoriaController.class,
            AdminReporteController.class,
            AdminExportacionController.class,
            AdminSearchController.class,
            AdminMonitoreoController.class,
            AdminMonitoreoApiController.class,
            AdminNotificacionApiController.class,
            AdminCuestionarioController.class,
            AdminSesionPdfController.class
    );

    @Test
    @DisplayName("method security está habilitado")
    void methodSecurityIsEnabled() {
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class)).isTrue();
    }

    @Test
    @DisplayName("cada controlador administrativo exige ROLE_ADMIN")
    void everyAdminControllerRequiresAdminRol() {
        assertThat(ADMIN_CONTROLLERS).allSatisfy(controller -> {
            PreAuthorize annotation = controller.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("falta @PreAuthorize en %s", controller.getSimpleName())
                    .isNotNull();
            assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
        });
    }
}
