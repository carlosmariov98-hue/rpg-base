package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();
        Personaje[] finalistas = new Personaje[2]; // memoria compartida: cada hilo escribe su casilla

        Thread semifinalA = new Thread(
                () -> finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1)), "semifinal-A");
        Thread semifinalB = new Thread(
                () -> finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3)), "semifinal-B");

        System.out.printf("Estado antes de start(): A=%s B=%s%n", semifinalA.getState(), semifinalB.getState());
        semifinalA.start();
        semifinalB.start();
        System.out.printf("Estado despues de start(): A=%s B=%s%n", semifinalA.getState(), semifinalB.getState());

        try {
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Torneo interrumpido mientras esperaba las semifinales", e);
        }
        System.out.printf("Estado despues de join(): A=%s B=%s%n", semifinalA.getState(), semifinalB.getState());

        // Solo ahora es seguro leer los ganadores: join() garantiza que ambos hilos terminaron
        if (finalistas[0] == null || finalistas[1] == null) {
            throw new IllegalStateException("Una semifinal fallo y no hay finalista");
        }
        return simularFinal(finalistas[0], finalistas[1]);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
