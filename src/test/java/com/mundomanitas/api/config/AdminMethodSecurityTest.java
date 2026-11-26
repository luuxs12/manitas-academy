package com.mundomanitas.api.config;

import com.mundomanitas.api.controller.admin.AdminAuditController;
import com.mundomanitas.api.controller.admin.AdminCertificateController;
import com.mundomanitas.api.controller.admin.AdminContentController;
import com.mundomanitas.api.controller.admin.AdminController;
import com.mundomanitas.api.controller.admin.AdminCourseController;
import com.mundomanitas.api.controller.admin.AdminExportController;
import com.mundomanitas.api.controller.admin.AdminNotificationApiController;
import com.mundomanitas.api.controller.admin.AdminQuizController;
import com.mundomanitas.api.controller.admin.AdminReportController;
import com.mundomanitas.api.controller.admin.AdminSearchController;
import com.mundomanitas.api.controller.admin.AdminSessionPdfController;
import com.mundomanitas.api.controller.admin.AdminUserController;
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
            AdminUserController.class,
            AdminCourseController.class,
            AdminContentController.class,
            AdminCertificateController.class,
            AdminAuditController.class,
            AdminReportController.class,
            AdminExportController.class,
            AdminSearchController.class,
            AdminNotificationApiController.class,
            AdminQuizController.class,
            AdminSessionPdfController.class
    );

    @Test
    @DisplayName("method security está habilitado")
    void methodSecurityIsEnabled() {
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class)).isTrue();
    }

    @Test
    @DisplayName("cada controlador administrativo exige ROLE_ADMIN")
    void everyAdminControllerRequiresAdminRole() {
        assertThat(ADMIN_CONTROLLERS).allSatisfy(controller -> {
            PreAuthorize annotation = controller.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("falta @PreAuthorize en %s", controller.getSimpleName())
                    .isNotNull();
            assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
        });
    }
}
