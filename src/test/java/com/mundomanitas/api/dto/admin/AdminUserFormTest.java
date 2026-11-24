package com.mundomanitas.api.dto.admin;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("El panel administrativo solo acepta los roles STUDENT y ADMIN")
    void rejectsTeacherRole() {
        AdminUserForm form = new AdminUserForm(
                "Ana Pérez", "ana@correo.com", "TEACHER", true, null);

        assertThat(validator.validate(form))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("role"));
    }

    @Test
    @DisplayName("Los roles STUDENT y ADMIN siguen siendo válidos")
    void acceptsStudentAndAdminRoles() {
        for (String role : new String[]{"STUDENT", "ADMIN"}) {
            AdminUserForm form = new AdminUserForm(
                    "Ana Pérez", "ana@correo.com", role, true, null);

            assertThat(validator.validate(form)).isEmpty();
        }
    }
}
