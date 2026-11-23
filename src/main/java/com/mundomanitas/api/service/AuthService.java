package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.RegisterForm;
import com.mundomanitas.api.model.User;

/**
 * Reglas de las cuentas: registrar una persona nueva sin repetir correos.
 */
public interface AuthService {

    User register(RegisterForm form);
}
