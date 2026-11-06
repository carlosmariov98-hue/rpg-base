package com.rpg.view;

import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;

/** Menu principal del juego: aqui vive lo que antes estaba en Main. */
public class MenuPrincipalUI {

    private final Consola consola;
    private final MotorJuego motor;
    private final HeroeUI heroeUI;

    public MenuPrincipalUI(Consola consola, MotorJuego motor, HeroeUI heroeUI) {
        this.consola = consola;
        this.motor = motor;
        this.heroeUI = heroeUI;
    }

    public void iniciar() {
        motor.iniciarAutoGuardado();
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", 0, 9);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.cargarProgreso();
                case 5 -> motor.listarArchivos();
                case 6 -> motor.verHistorial();
                case 7 -> motor.borrarArchivo();
                case 8 -> heroeUI.iniciar();
                case 9 -> heroeUI.cargarHeroeYCombatir();
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Cargar partida");
        System.out.println("5. Ver archivos guardados");
        System.out.println("6. Ver historial de combates");
        System.out.println("7. Borrar archivo");
        System.out.println("8. Gestionar heroes (BD)");
        System.out.println("9. Cargar heroe desde BD y combatir");
        System.out.println("0. Salir");
    }
}