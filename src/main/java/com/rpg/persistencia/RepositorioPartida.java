package com.rpg.persistencia;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Lee, escribe, lista y borra los archivos de la partida dentro de una carpeta. */
public class RepositorioPartida {

    private final File carpeta;

    public RepositorioPartida(String ruta) {
        this.carpeta = new File(ruta);
    }

    public File archivo(String nombre) {
        return new File(carpeta, nombre);
    }

    /** Escribe primero a un .tmp y luego lo renombra: un fallo a mitad no deja un .dat corrupto. */
    public void guardarObjeto(String nombreArchivo, Serializable objeto) throws IOException {
        asegurarCarpeta();
        File temporal = archivo(nombreArchivo + ".tmp");
        try (ObjectOutputStream salida = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(temporal)))) {
            salida.writeObject(objeto);
        } catch (IOException | RuntimeException e) {
            temporal.delete();
            throw e;
        }
        Files.move(temporal.toPath(), archivo(nombreArchivo).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public DatosPartida cargarObjeto(String nombreArchivo) throws IOException, ClassNotFoundException {
        File origen = archivo(nombreArchivo);
        if (!origen.isFile()) {
            throw new FileNotFoundException("No existe " + origen.getPath());
        }
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(origen)))) {
            Object leido = entrada.readObject();
            if (leido instanceof DatosPartida datos) {
                return datos;
            }
            throw new InvalidClassException("El archivo no contiene una partida valida");
        }
    }

    public void escribirTexto(String nombreArchivo, String contenido, boolean agregar) throws IOException {
        asegurarCarpeta();
        try (BufferedWriter escritor = new BufferedWriter(
                new FileWriter(archivo(nombreArchivo), StandardCharsets.UTF_8, agregar))) {
            escritor.write(contenido);
        }
    }

    public List<File> listar() {
        File[] archivos = carpeta.listFiles(File::isFile);
        if (archivos == null) {
            return List.of();
        }
        Arrays.sort(archivos, Comparator.comparing(File::getName));
        return Arrays.asList(archivos);
    }

    public boolean borrar(String nombreArchivo) {
        return archivo(nombreArchivo).delete();
    }

    private void asegurarCarpeta() throws IOException {
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getPath());
        }
    }
}