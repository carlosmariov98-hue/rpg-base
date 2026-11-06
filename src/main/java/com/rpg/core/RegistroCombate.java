package com.rpg.core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida. */
public class RegistroCombate {

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    // TALLER ARCHIVOS: aqui iria volcar el registro de combate a un archivo .txt (BufferedWriter + FileWriter,
    // con append si quieres conservar combates anteriores) y leerlo de vuelta con BufferedReader.
    public synchronized String getTexto() {
        return texto.toString();
    }

    /** Vuelca el registro a un .txt. Con agregar=true conserva los combates anteriores. */
    public synchronized void volcarAArchivo(File archivo, boolean agregar) throws IOException {
        File padre = archivo.getParentFile();
        if (padre != null && !padre.exists() && !padre.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + padre.getPath());
        }
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, StandardCharsets.UTF_8, agregar))) {
            escritor.write(texto.toString());
            escritor.newLine();
        }
    }

    public static String leerArchivo(File archivo) throws IOException {
        StringBuilder contenido = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                contenido.append(linea).append(System.lineSeparator());
            }
        }
        return contenido.toString();
    }
}
