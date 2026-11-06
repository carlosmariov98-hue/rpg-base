package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Unica clase que lee config.txt y abre conexiones. Ninguna otra clase conoce la clave. */
public class ConexionDB {

    private static final String ARCHIVO_CONFIG = "config.txt";

    private ConexionDB() {
    }

    public static Connection obtener() throws DaoException {
        Map<String, String> config = leerConfig();
        String url = exigir(config, "url");
        String usuario = exigir(config, "usuario");
        String clave = exigir(config, "clave");
        try {
            // URL de ejemplo: jdbc:mysql://localhost:3306/rpg_grupal_db
            //   jdbc           -> protocolo: Java Database Connectivity
            //   mysql          -> subprotocolo: el motor (selecciona el driver mysql-connector-j)
            //   localhost      -> host: el equipo donde corre el servidor MySQL
            //   3306           -> puerto en el que escucha MySQL
            //   rpg_grupal_db  -> esquema (base de datos) al que nos conectamos
            return DriverManager.getConnection(url, usuario, clave);
        } catch (SQLException e) {
            throw new DaoException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> leerConfig() throws DaoException {
        File archivo = new File(ARCHIVO_CONFIG);
        Map<String, String> config = new HashMap<>();
        try {
            if (!archivo.exists()) {
                throw new FileNotFoundException("No se encontro " + ARCHIVO_CONFIG
                        + ". Copia config.example.txt como config.txt y completa tus datos.");
            }
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty()) {
                        continue;
                    }
                    String[] partes = linea.split("=", 2);
                    if (partes.length == 2) {
                        config.put(partes[0].trim(), partes[1].trim());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new DaoException(e.getMessage(), e);
        } catch (IOException e) {
            throw new DaoException("Error al leer " + ARCHIVO_CONFIG + ": " + e.getMessage(), e);
        }
        return config;
    }

    private static String exigir(Map<String, String> config, String clave) throws DaoException {
        String valor = config.get(clave);
        if (valor == null || valor.isEmpty()) {
            throw new DaoException("Falta la clave '" + clave + "' en " + ARCHIVO_CONFIG, null);
        }
        return valor;
    }
}
