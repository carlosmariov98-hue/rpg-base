package com.rpg.concurrency;

import com.rpg.entities.Guerrero;
import com.rpg.entities.Personaje;

/** Dos hilos golpean al mismo personaje 100000 veces cada uno. Con la vida protegida debe dar 800000. */
public class PruebaCarrera {

    public static void main(String[] args) throws InterruptedException {
        Personaje p = new Guerrero("Prueba");
        p.setVidaMaxima(1_000_000);
        p.restaurarVida();
        Runnable golpes = () -> {
            for (int i = 0; i < 100_000; i++) {
                p.recibirDanio(1);
            }
        };
        Thread a = new Thread(golpes);
        Thread b = new Thread(golpes);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Esperado: 800000 | Real: " + p.getVida());
    }
}