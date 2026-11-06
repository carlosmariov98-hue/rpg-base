package com.rpg.model;

/** Entidad POJO: una fila de la tabla heroes. */
public class Heroe {

    public static final String TIPO_GUERRERO = "GUERRERO";
    public static final String TIPO_MAGO = "MAGO";
    public static final int LARGO_MAXIMO_NOMBRE = 15;

    private int id;
    private String nombre;
    private String tipo;
    private int vidaMaxima;
    private int nivel;

    /** Heroe nuevo, aun sin id (lo asigna la BD). */
    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        this(0, nombre, tipo, vidaMaxima, nivel);
    }

    /** Heroe leido de la BD. */
    public Heroe(int id, String nombre, String tipo, int vidaMaxima, int nivel) {
        this.id = id;
        setNombre(nombre);
        setTipo(tipo);
        setVidaMaxima(vidaMaxima);
        setNivel(nivel);
    }

    public static boolean esTipoValido(String tipo) {
        return tipo != null
                && (tipo.trim().equalsIgnoreCase(TIPO_GUERRERO) || tipo.trim().equalsIgnoreCase(TIPO_MAGO));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (nombre.trim().length() > LARGO_MAXIMO_NOMBRE) {
            throw new IllegalArgumentException("El nombre admite maximo " + LARGO_MAXIMO_NOMBRE + " caracteres");
        }
        this.nombre = nombre.trim();
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        if (!esTipoValido(tipo)) {
            throw new IllegalArgumentException("Tipo invalido: use GUERRERO o MAGO");
        }
        this.tipo = tipo.trim().toUpperCase();
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel minimo es 1");
        }
        this.nivel = nivel;
    }
}
