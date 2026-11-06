package com.rpg.dao;

/** Error de acceso a datos con un mensaje apto para mostrar al usuario. */
public class DaoException extends Exception {

    public DaoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}