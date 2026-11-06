package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.controller.Respuesta;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.entities.Personaje;
import com.rpg.model.Heroe;

import java.util.List;

/** Menu de heroes por consola. Sin una sola linea de JDBC. */
public class HeroeUI {

    private final Consola consola;
    private final HeroeController controller;
    private final MotorJuego motor;

    public HeroeUI(Consola consola, HeroeController controller, MotorJuego motor) {
        this.consola = consola;
        this.controller = controller;
        this.motor = motor;
    }

    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", 0, 5);
            switch (opcion) {
                case 1 -> registrar();
                case 2 -> listar();
                case 3 -> buscarPorTipo();
                case 4 -> actualizarNivel();
                case 5 -> eliminar();
                default -> System.out.println("Volviendo al menu principal.");
            }
        } while (opcion != 0);
    }

    /** Opcion "Cargar heroe desde BD": lista, elige y lo usa en un combate rapido. */
    public void cargarHeroeYCombatir() {
        Respuesta<List<Heroe>> lista = controller.listar();
        if (!lista.exito()) {
            System.out.println(lista.mensaje());
            return;
        }
        if (lista.datos().isEmpty()) {
            System.out.println("No hay heroes guardados. Registra uno primero.");
            return;
        }
        imprimirTabla(lista.datos());
        int id = consola.leerEntero("Id del heroe (0 = cancelar): ", 0, Integer.MAX_VALUE);
        if (id == 0) {
            return;
        }
        Respuesta<Personaje> cargado = controller.cargarComoPersonaje(id);
        System.out.println(cargado.mensaje());
        if (cargado.exito()) {
            motor.combateRapidoConHeroe(cargado.datos());
        }
    }

    private void mostrarMenu() {
        System.out.println("\n=== REGISTRO DE HEROES ===");
        System.out.println("1. Registrar heroe");
        System.out.println("2. Listar heroes");
        System.out.println("3. Buscar por tipo");
        System.out.println("4. Actualizar nivel");
        System.out.println("5. Eliminar heroe");
        System.out.println("0. Volver");
    }

    private void registrar() {
        // La vista solo pide los datos; las reglas las valida el controller
        String nombre = consola.leerLinea("Nombre: ");
        String tipo = consola.leerLinea("Tipo (GUERRERO o MAGO): ");
        int vida = consola.leerEntero("Vida maxima: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
        int nivel = consola.leerEntero("Nivel: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println(controller.registrar(nombre, tipo, vida, nivel).mensaje());
    }

    private void listar() {
        mostrarLista(controller.listar());
    }

    private void buscarPorTipo() {
        mostrarLista(controller.buscarPorTipo(consola.leerLinea("Tipo (GUERRERO o MAGO): ")));
    }

    private void actualizarNivel() {
        int id = consola.leerEntero("Id del heroe: ", 1, Integer.MAX_VALUE);
        int nivel = consola.leerEntero("Nuevo nivel: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println(controller.actualizarNivel(id, nivel).mensaje());
    }

    private void eliminar() {
        int id = consola.leerEntero("Id del heroe a eliminar: ", 1, Integer.MAX_VALUE);
        System.out.println(controller.eliminar(id).mensaje());
    }

    private void mostrarLista(Respuesta<List<Heroe>> respuesta) {
        if (!respuesta.exito()) {
            System.out.println(respuesta.mensaje());
            return;
        }
        imprimirTabla(respuesta.datos());
    }

    private void imprimirTabla(List<Heroe> heroes) {
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes para mostrar.");
            return;
        }
        System.out.printf("%-4s %-15s %-9s %-6s %-5s%n", "ID", "NOMBRE", "TIPO", "VIDA", "NIVEL");
        for (Heroe heroe : heroes) {
            System.out.printf("%-4d %-15s %-9s %-6d %-5d%n", heroe.getId(), heroe.getNombre(),
                    heroe.getTipo(), heroe.getVidaMaxima(), heroe.getNivel());
        }
    }
}