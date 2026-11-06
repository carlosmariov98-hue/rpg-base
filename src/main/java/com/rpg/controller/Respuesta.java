package com.rpg.controller;

/** Lo que el controller le devuelve a la vista: exito, mensaje y datos opcionales. */
public record Respuesta<T>(boolean exito, String mensaje, T datos) {

    public static <T> Respuesta<T> ok(String mensaje, T datos) {
        return new Respuesta<>(true, mensaje, datos);
    }

    public static <T> Respuesta<T> error(String mensaje) {
        return new Respuesta<>(false, mensaje, null);
    }
}