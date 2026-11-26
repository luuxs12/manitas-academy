package com.mundomanitas.api.dto.admin;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUsuarioFormularioTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("El panel administrativo solo acepta los roles STUDENT y ADMIN")
    void rejectsTeacherRol() {
        AdminUsuarioFormulario formulario = new AdminUsuarioFormulario(
                "Ana Pérez", "ana@correo.com", "TEACHER", true, null);

        assertThat(validator.validate(formulario))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("rol"));
    }

    @Test
    @DisplayName("Los roles STUDENT y ADMIN siguen siendo válidos")
    void acceptsEstudianteAndAdminRoles() {
        for (String rol : new String[]{"STUDENT", "ADMIN"}) {
            AdminUsuarioFormulario formulario = new AdminUsuarioFormulario(
                    "Ana Pérez", "ana@correo.com", rol, true, null);

            assertThat(validator.validate(formulario)).isEmpty();
        }
    }
}
