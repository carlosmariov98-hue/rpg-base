package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Demuestra ruta relativa y absoluta, createNewFile, y la diferencia entre File.delete y Files.deleteIfExists. */
public class DemoArchivos {

    public static void main(String[] args) {
        File relativo = new File("partidas/demo.txt");
        System.out.println("Ruta relativa: " + relativo.getPath() + " (depende de donde se ejecute el programa)");
        System.out.println("Ruta absoluta: " + relativo.getAbsolutePath() + " (siempre apunta al mismo sitio)");

        File carpeta = relativo.getParentFile();
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            System.out.println("No se pudo crear la carpeta " + carpeta.getPath());
            return;
        }
        try {
            System.out.println("createNewFile: " + relativo.createNewFile()); // false si ya existia
            System.out.printf("Existe: %b | Nombre: %s | Tamano: %d bytes%n",
                    relativo.exists(), relativo.getName(), relativo.length());

            // File.delete(): solo devuelve true/false, sin decir por que fallo
            System.out.println("File.delete: " + relativo.delete());
            System.out.println("File.delete otra vez: " + relativo.delete());

            // Files.deleteIfExists: false si no existia; si falla de verdad lanza IOException con la causa
            System.out.println("Files.deleteIfExists (ya no existe): " + Files.deleteIfExists(relativo.toPath()));
        } catch (IOException e) {
            System.out.println("Error de archivo: " + e.getMessage());
        }
    }
}