package com.rpg.concurrency;

import java.util.concurrent.locks.ReentrantLock;

/** Version con ReentrantLock de la logica de Personaje.recibirDanio (investigacion obligatoria). */
public class VidaConLock {

    private final ReentrantLock candado = new ReentrantLock();
    private final int vidaMaxima;
    private int vida;

    public VidaConLock(int vidaMaxima) {
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
    }

    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        candado.lock();
        try {
            vida = Math.max(0, Math.min(vida - cantidad, vidaMaxima));
        } finally {
            candado.unlock(); // en el finally: si no, un error deja el lock tomado para siempre
        }
    }

    public int getVida() {
        candado.lock();
        try {
            return vida;
        } finally {
            candado.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        VidaConLock v = new VidaConLock(1_000_000);
        Runnable golpes = () -> {
            for (int i = 0; i < 100_000; i++) {
                v.recibirDanio(1);
            }
        };
        Thread a = new Thread(golpes);
        Thread b = new Thread(golpes);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Esperado: 800000 | Real: " + v.getVida());
    }
}
