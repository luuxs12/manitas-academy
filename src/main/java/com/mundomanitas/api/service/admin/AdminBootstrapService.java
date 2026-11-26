package com.mundomanitas.api.service.admin;

/** Asegura que la cuenta administradora exista en cualquier base donde arranque la app. */
public interface AdminBootstrapService {

    void ensureAdmin(String email, String contrasena);
}
