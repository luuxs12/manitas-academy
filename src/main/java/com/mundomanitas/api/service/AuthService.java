package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.RegistroFormulario;
import com.mundomanitas.api.model.Usuario;

/**
 * Reglas de las cuentas: registrar una persona nueva sin repetir correos.
 */
public interface AuthService {

    Usuario registro(RegistroFormulario formulario);
}
