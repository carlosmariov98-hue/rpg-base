package com.rpg;

import com.rpg.controller.HeroeController;
import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.dao.HeroeDAO;
import com.rpg.view.HeroeUI;
import com.rpg.view.MenuPrincipalUI;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Consola consola = new Consola(scanner); // un solo Scanner para todo el programa
            MotorJuego motor = new MotorJuego(consola);
            HeroeUI heroeUI = new HeroeUI(consola, new HeroeController(new HeroeDAO()), motor);
            new MenuPrincipalUI(consola, motor, heroeUI).iniciar();
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }
}