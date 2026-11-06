package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Foto del estado de la partida: lo que se escribe con ObjectOutputStream y se lee al cargar. */
public class DatosPartida implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final LinkedHashMap<String, Integer> derrotados;
    private final String fecha = LocalDateTime.now().withNano(0).toString();

    public DatosPartida(PerfilJugador perfil, Gremio gremio, Map<String, Integer> derrotados) {
        this.perfil = perfil;
        this.gremio = gremio;
        this.derrotados = new LinkedHashMap<>(derrotados);
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Map<String, Integer> getDerrotados() {
        return derrotados;
    }

    public String getFecha() {
        return fecha;
    }
}
