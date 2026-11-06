package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.DatosPartida;
import com.rpg.persistencia.RepositorioPartida;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.io.NotSerializableException;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int SEGUNDOS_TURNO = 10;
    private static final long INTERVALO_AUTOGUARDADO_MS = 30_000L;
    private static final String CARPETA_PARTIDAS = "partidas";
    private static final String ARCHIVO_LOG = "combates.log";

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final GestorTurno gestorTurno = new GestorTurno();
    private final RepositorioPartida repositorio = new RepositorioPartida(CARPETA_PARTIDAS);
    private final Object candadoGuardado = new Object();

    // Sin final: al cargar una partida estos dos objetos se reemplazan por los leidos del disco
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);

    private volatile boolean tiempoAgotado;
    private volatile String ultimoGuardado = "";

    public MotorJuego(Consola consola) {
        this.consola = consola;
    }

    public void iniciarAutoGuardado() {
        Thread hilo = new Thread(
                new AutoGuardadoDaemon(INTERVALO_AUTOGUARDADO_MS, () -> guardar(false)), "auto-guardado");
        hilo.setDaemon(true); // siempre ANTES de start(), si no lanza IllegalThreadStateException
        hilo.start();
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        combatirCon(crearJugador());
    }

    /** El heroe viene de la BD: solo falta el kit y el gremio. */
    public void combateRapidoConHeroe(Personaje jugador) {
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        combatirCon(jugador);
    }

    private void combatirCon(Personaje jugador) {
        registro.limpiar();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);

        cerrarCombate(jugador, enemigo, huyo);
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        guardarProgreso();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- archivos: cargar, listar, historial, borrar

    public void cargarProgreso() {
        try {
            DatosPartida datos = repositorio.cargarObjeto("partida.dat");
            synchronized (candadoGuardado) {
                perfil = datos.getPerfil();
                gremio = datos.getGremio();
                bestiario.setDerrotados(datos.getDerrotados());
            }
            System.out.printf("Partida cargada (%s): %d victorias, %d derrotas, %d miembros en el gremio.%n",
                    datos.getFecha(), perfil.getVictorias(), perfil.getDerrotas(), gremio.getMiembros().size());
        } catch (FileNotFoundException e) {
            System.out.println("No hay ninguna partida guardada todavia.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No se pudo cargar la partida: " + e.getMessage());
        }
    }

    public void listarArchivos() {
        List<File> archivos = repositorio.listar();
        if (archivos.isEmpty()) {
            System.out.println("No hay archivos guardados.");
            return;
        }
        System.out.println("\n=== ARCHIVOS GUARDADOS ===");
        for (File archivo : archivos) {
            System.out.printf("  %-20s %7d bytes%n", archivo.getName(), archivo.length());
        }
    }

    public void verHistorial() {
        try {
            String texto = RegistroCombate.leerArchivo(repositorio.archivo(ARCHIVO_LOG));
            System.out.println(texto.isBlank() ? "El historial esta vacio." : texto);
        } catch (FileNotFoundException e) {
            System.out.println("Todavia no hay historial de combates.");
        } catch (IOException e) {
            System.out.println("No se pudo leer el historial: " + e.getMessage());
        }
    }

    public void borrarArchivo() {
        List<File> archivos = repositorio.listar();
        if (archivos.isEmpty()) {
            System.out.println("No hay archivos para borrar.");
            return;
        }
        for (int i = 0; i < archivos.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, archivos.get(i).getName());
        }
        int eleccion = consola.leerEntero("Archivo a borrar (0 = cancelar): ", 0, archivos.size());
        if (eleccion == 0) {
            return;
        }
        File elegido = archivos.get(eleccion - 1);
        String confirmacion = consola.leerLinea("Borrar " + elegido.getName() + "? (s/n): ");
        if (confirmacion.equalsIgnoreCase("s")) {
            System.out.println(repositorio.borrar(elegido.getName()) ? "Archivo borrado." : "No se pudo borrar.");
        }
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                case TIEMPO_AGOTADO -> {
                    registro.agregar("%s pierde el turno por tardar demasiado.", jugador.getNombre());
                    yield true;
                }
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /** Pide la accion con un limite de tiempo: si se agota, el turno se pierde. */
    private Accion pedirAccion() {
        tiempoAgotado = false;
        gestorTurno.iniciar(SEGUNDOS_TURNO, () -> {
            tiempoAgotado = true;
            System.out.println("\n>> Tiempo agotado! Escribe una opcion valida para continuar.");
        });
        System.out.printf("Tienes %d segundos.%n", SEGUNDOS_TURNO);
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");
        int opcion = consola.leerEntero("Accion: ", 0, 4);
        gestorTurno.cancelar(); // respondio: se interrumpe el hilo del temporizador
        if (tiempoAgotado) {
            return Accion.TIEMPO_AGOTADO;
        }
        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d turnos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        jugador.detenerEfectos();
        enemigo.detenerEfectos();
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    /** Guardado manual: escribe partida.txt / partida.dat y agrega el combate al historial. */
    public void guardarProgreso() {
        guardar(true);
        try {
            registro.volcarAArchivo(repositorio.archivo(ARCHIVO_LOG), true);
        } catch (IOException e) {
            System.out.println("[Error al guardar el historial] " + e.getMessage());
        }
    }

    /** @param manual true = partida.*, false = autoguardado.* (asi el auto-guardado no pisa tu partida) */
    private void guardar(boolean manual) {
        boolean enDisco;
        synchronized (candadoGuardado) {
            StringBuilder contenido = new StringBuilder();
            contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
            contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
            contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
            contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
            contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                    .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
            contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
            contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
            contenido.append(registro.getTexto());
            ultimoGuardado = contenido.toString();
            enDisco = escribirEnDisco(manual ? "partida" : "autoguardado");
        }
        if (manual && enDisco) {
            System.out.println("[Progreso guardado en memoria y en disco]");
        }
    }

    private boolean escribirEnDisco(String base) {
        try {
            repositorio.escribirTexto(base + ".txt", ultimoGuardado, false);
            repositorio.guardarObjeto(base + ".dat", new DatosPartida(perfil, gremio, bestiario.getDerrotados()));
            return true;
        } catch (NotSerializableException e) {
            System.out.println("[Error al guardar] la clase " + e.getMessage() + " no es Serializable.");
            return false;
        } catch (IOException e) {
            System.out.println("[Error al guardar] " + e.getMessage());
            return false;
        }
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }
}