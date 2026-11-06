package com.rpg.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Agregacion: el gremio guarda referencias a personajes que existen por su cuenta. */
// TALLER ARCHIVOS: candidata a Serializable (guarda la lista de Personaje, que tambien debe serlo).
public class Gremio implements java.io.Serializable {
    private static final long serialVersionUID = 1L;


    private final String nombre;
    private final List<Personaje> miembros = new ArrayList<>();

    public Gremio(String nombre) {
        this.nombre = nombre;
    }

    public void agregarMiembro(Personaje personaje) {
        if (personaje == null || miembros.contains(personaje)) {
            return;
        }
        miembros.add(personaje);
        if (personaje.getGremio() != this) {
            personaje.unirseAGremio(this);
        }
    }

    public void removerMiembro(Personaje personaje) {
        if (!miembros.remove(personaje)) {
            return;
        }
        if (personaje.getGremio() == this) {
            personaje.abandonarGremio();
        }
    }

    public String getNombre() {
        return nombre;
    }

    public List<Personaje> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }
}
