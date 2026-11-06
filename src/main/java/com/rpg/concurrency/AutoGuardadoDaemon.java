package com.rpg.concurrency;

import java.util.ConcurrentModificationException;

/** Ejecuta un guardado periodico. Debe lanzarse en un hilo daemon: no impide que la JVM cierre. */
public class AutoGuardadoDaemon implements Runnable {

    private final long intervaloMs;
    private final Runnable guardado;

    public AutoGuardadoDaemon(long intervaloMs, Runnable guardado) {
        this.intervaloMs = intervaloMs;
        this.guardado = guardado;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                guardado.run();
            } catch (ConcurrentModificationException e) {
                System.out.println("[Auto-guardado] estado en cambio, se reintenta en el siguiente ciclo");
            }
        }
    }
}
