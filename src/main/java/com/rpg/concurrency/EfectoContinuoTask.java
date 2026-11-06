package com.rpg.concurrency;

import com.rpg.entities.Personaje;

/** Aplica un efecto DoT/HoT en su propio hilo, un tick por intervalo, hasta que termina o lo interrumpen. */
public class EfectoContinuoTask implements Runnable {

    private final Personaje objetivo;
    private final String nombreEfecto;
    private final long intervaloMs;

    public EfectoContinuoTask(Personaje objetivo, String nombreEfecto, long intervaloMs) {
        this.objetivo = objetivo;
        this.nombreEfecto = nombreEfecto;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervaloMs);
                } catch (InterruptedException e) {
                    objetivo.efectoInterrumpido(nombreEfecto);
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!objetivo.tickEfecto(nombreEfecto)) {
                    break;
                }
            }
        } finally {
            // Sea cual sea la salida (fin natural, interrupcion o muerte) el personaje suelta este hilo
            objetivo.efectoInterrumpido(nombreEfecto);
        }
    }
}