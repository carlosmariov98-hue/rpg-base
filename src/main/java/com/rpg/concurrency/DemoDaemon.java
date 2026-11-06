package com.rpg.concurrency;

/**
 * Hilo de usuario: la JVM no termina mientras alguno siga vivo.
 * Hilo daemon: la JVM termina cuando solo quedan daemons, y los corta de golpe.
 */
public class DemoDaemon {

    public static void main(String[] args) throws InterruptedException {
        Thread hilo = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    System.out.println("daemon vivo");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
        try {
            hilo.setDaemon(false); // despues de start() no se puede cambiar
        } catch (IllegalThreadStateException e) {
            System.out.println("Esperado: " + e);
        }
        Thread.sleep(1200);
        System.out.println("main termina y la JVM cierra aunque el daemon siga vivo");
    }
}
