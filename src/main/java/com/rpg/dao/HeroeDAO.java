package com.rpg.dao;

import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Toda la logica JDBC de la tabla heroes. Siempre PreparedStatement y try-with-resources. */
public class HeroeDAO {

    private static final int CODIGO_DUPLICADO = 1062; // error de MySQL por UNIQUE repetido

    private static final String SQL_INSERTAR =
            "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
    private static final String SQL_LISTAR =
            "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes ORDER BY id";
    private static final String SQL_POR_TIPO =
            "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ? ORDER BY id";
    private static final String SQL_ACTUALIZAR_NIVEL = "UPDATE heroes SET nivel = ? WHERE id = ?";
    private static final String SQL_ELIMINAR = "DELETE FROM heroes WHERE id = ?";

    /** @return filas insertadas */
    public int insertar(Heroe heroe) throws DaoException {
        try (Connection con = ConexionDB.obtener();
             PreparedStatement ps = con.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, heroe.getNombre());
            ps.setString(2, heroe.getTipo());
            ps.setInt(3, heroe.getVidaMaxima());
            ps.setInt(4, heroe.getNivel());
            return ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == CODIGO_DUPLICADO) {
                throw new DaoException("Ya existe un heroe con el nombre " + heroe.getNombre(), e);
            }
            throw new DaoException("No se pudo guardar el heroe: " + e.getMessage(), e);
        }
    }

    public List<Heroe> listar() throws DaoException {
        List<Heroe> heroes = new ArrayList<>();
        try (Connection con = ConexionDB.obtener();
             PreparedStatement ps = con.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                heroes.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo leer la lista de heroes: " + e.getMessage(), e);
        }
        return heroes;
    }

    public List<Heroe> buscarPorTipo(String tipo) throws DaoException {
        List<Heroe> heroes = new ArrayList<>();
        try (Connection con = ConexionDB.obtener();
             PreparedStatement ps = con.prepareStatement(SQL_POR_TIPO)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    heroes.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar por tipo: " + e.getMessage(), e);
        }
        return heroes;
    }

    /** @return filas afectadas (0 si no existe ese id) */
    public int actualizarNivel(int id, int nivel) throws DaoException {
        try (Connection con = ConexionDB.obtener();
             PreparedStatement ps = con.prepareStatement(SQL_ACTUALIZAR_NIVEL)) {
            ps.setInt(1, nivel);
            ps.setInt(2, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo actualizar el nivel: " + e.getMessage(), e);
        }
    }

    /** @return filas afectadas (0 si no existe ese id) */
    public int eliminar(int id) throws DaoException {
        try (Connection con = ConexionDB.obtener();
             PreparedStatement ps = con.prepareStatement(SQL_ELIMINAR)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("No se pudo eliminar el heroe: " + e.getMessage(), e);
        }
    }

    private Heroe mapear(ResultSet rs) throws SQLException {
        return new Heroe(rs.getInt("id"), rs.getString("nombre"), rs.getString("tipo"),
                rs.getInt("vida_maxima"), rs.getInt("nivel"));
    }
}