package com.rpg.controller;

import com.rpg.dao.DaoException;
import com.rpg.dao.HeroeDAO;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.util.List;

/** Valida, llama al DAO y decide el mensaje. No imprime ni usa JDBC. */
public class HeroeController {

    private final HeroeDAO dao;

    public HeroeController(HeroeDAO dao) {
        this.dao = dao;
    }

    public Respuesta<Void> registrar(String nombre, String tipo, int vidaMaxima, int nivel) {
        if (nombre == null || nombre.isBlank()) {
            return Respuesta.error("El nombre no puede estar vacio.");
        }
        if (nombre.trim().length() > Heroe.LARGO_MAXIMO_NOMBRE) {
            return Respuesta.error("El nombre admite maximo " + Heroe.LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        if (!Heroe.esTipoValido(tipo)) {
            return Respuesta.error("Tipo invalido: escriba GUERRERO o MAGO.");
        }
        if (vidaMaxima <= 0) {
            return Respuesta.error("La vida maxima debe ser mayor que 0.");
        }
        if (nivel < 1) {
            return Respuesta.error("El nivel minimo es 1.");
        }
        try {
            dao.insertar(new Heroe(nombre, tipo, vidaMaxima, nivel));
            return Respuesta.ok("Heroe " + nombre.trim() + " guardado.", null);
        } catch (DaoException e) {
            return Respuesta.error(e.getMessage());
        }
    }

    public Respuesta<List<Heroe>> listar() {
        try {
            List<Heroe> heroes = dao.listar();
            return Respuesta.ok(heroes.size() + " heroe(s) encontrado(s).", heroes);
        } catch (DaoException e) {
            return Respuesta.error(e.getMessage());
        }
    }

    public Respuesta<List<Heroe>> buscarPorTipo(String tipo) {
        if (!Heroe.esTipoValido(tipo)) {
            return Respuesta.error("Tipo invalido: escriba GUERRERO o MAGO.");
        }
        try {
            List<Heroe> heroes = dao.buscarPorTipo(tipo.trim().toUpperCase());
            return Respuesta.ok(heroes.size() + " heroe(s) de tipo " + tipo.trim().toUpperCase() + ".", heroes);
        } catch (DaoException e) {
            return Respuesta.error(e.getMessage());
        }
    }

    public Respuesta<Void> actualizarNivel(int id, int nivel) {
        if (nivel < 1) {
            return Respuesta.error("El nivel minimo es 1.");
        }
        try {
            if (dao.actualizarNivel(id, nivel) == 0) {
                return Respuesta.error("No existe un heroe con ese id.");
            }
            return Respuesta.ok("Nivel actualizado a " + nivel + ".", null);
        } catch (DaoException e) {
            return Respuesta.error(e.getMessage());
        }
    }

    public Respuesta<Void> eliminar(int id) {
        try {
            if (dao.eliminar(id) == 0) {
                return Respuesta.error("No existe un heroe con ese id.");
            }
            return Respuesta.ok("Heroe eliminado.", null);
        } catch (DaoException e) {
            return Respuesta.error(e.getMessage());
        }
    }

    /** Convierte una fila de la BD en un Guerrero o Mago listo para combatir. */
    public Respuesta<Personaje> cargarComoPersonaje(int id) {
        Respuesta<List<Heroe>> lista = listar();
        if (!lista.exito()) {
            return Respuesta.error(lista.mensaje());
        }
        for (Heroe heroe : lista.datos()) {
            if (heroe.getId() == id) {
                return Respuesta.ok("Heroe " + heroe.getNombre() + " cargado.", construir(heroe));
            }
        }
        return Respuesta.error("No existe un heroe con ese id.");
    }

    private Personaje construir(Heroe heroe) {
        Personaje personaje = heroe.getTipo().equals(Heroe.TIPO_GUERRERO)
                ? new Guerrero(heroe.getNombre())
                : new Mago(heroe.getNombre());
        personaje.setVidaMaxima(heroe.getVidaMaxima());
        personaje.restaurarVida(); // vida actual = la vida maxima guardada en la BD
        return personaje;
    }
}