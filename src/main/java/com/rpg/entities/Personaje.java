package com.rpg.entities;

import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// TALLER ARCHIVOS: para guardar la partida con ObjectOutputStream, Personaje debe volverse Serializable
// (con serialVersionUID). Piensa que campos son transient (por ejemplo hilosEfectos: un Thread no se
// serializa) y que clases anidadas (Inventario, Pocion, Arma, Gremio, EstadoEfecto, enums) tambien deben
// ser Serializable, o tendras NotSerializableException.
public abstract class Personaje implements ICombatiente, Serializable {

    private static final long serialVersionUID = 1L;

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;
    private static final long INTERVALO_EFECTO_MS = 1000L;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;
    private Gremio gremio;
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO);
    private final Set<String> habilidades = new HashSet<>();

    // Estado compartido entre hilos: todo acceso va dentro de metodos synchronized (candado = this)
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();
    private transient Map<String, Thread> hilosEfectos = new HashMap<>();
    private transient Queue<String> mensajesPendientes = new ConcurrentLinkedQueue<>();
    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida (synchronized: leer-calcular-escribir es una sola operacion atomica)

    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
    }

    /** @return vida realmente recuperada */
    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + cantidad);
        return vida - vidaAntes;
    }

    public synchronized void restaurarVida() {
        cancelarTodosLosHilos();
        efectos.clear();
        estado = EstadoPersonaje.VIVO;
        vida = vidaMaxima;
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT: cada uno corre en su propio hilo (EfectoContinuoTask)

    public synchronized void aplicarEfecto(EstadoEfecto efecto) {
        String clave = efecto.getNombre();
        cancelarHilo(clave); // si ya habia uno con ese nombre, se reemplaza
        EstadoEfecto copia = efecto.copia();
        efectos.put(clave, copia);
        if (copia.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        if (!estaVivo()) {
            return;
        }
        Thread hilo = new Thread(new EfectoContinuoTask(this, clave, INTERVALO_EFECTO_MS),
                "efecto-" + clave + "-" + nombre);
        hilo.setDaemon(true); // no debe impedir que la JVM cierre
        hilosEfectos.put(clave, hilo);
        hilo.start();
    }

    public synchronized boolean quitarEfecto(String nombreEfecto) {
        if (efectos.remove(nombreEfecto) == null) {
            return false;
        }
        cancelarHilo(nombreEfecto);
        normalizarEstado();
        return true;
    }

    public synchronized void quitarEfectosDanio() {
        for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
            if (efectos.get(nombreEfecto).esDanio()) {
                quitarEfecto(nombreEfecto);
            }
        }
    }

    /** Interrumpe todos los hilos de efectos y limpia los efectos (fin de combate). */
    public synchronized void detenerEfectos() {
        cancelarTodosLosHilos();
        efectos.clear();
        normalizarEstado();
    }

    /**
     * Lo invoca el hilo del efecto en cada tick.
     * @return true si el hilo debe seguir; false si debe terminar
     */
    public synchronized boolean tickEfecto(String nombreEfecto) {
        // Solo el hilo vigente de ese efecto puede aplicar ticks (evita ticks de un hilo ya reemplazado)
        if (hilosEfectos.get(nombreEfecto) != Thread.currentThread()) {
            return false;
        }
        EstadoEfecto efecto = efectos.get(nombreEfecto);
        if (efecto == null || !estaVivo()) {
            return false;
        }
        mensajesPendientes.add(aplicarTick(efecto));
        if (!estaVivo()) {
            return false;
        }
        efecto.consumirTurno();
        if (efecto.haTerminado()) {
            efectos.remove(nombreEfecto);
            hilosEfectos.remove(nombreEfecto);
            mensajesPendientes.add(nombre + ": termina " + nombreEfecto);
            normalizarEstado();
            return false;
        }
        return true;
    }

    /** Lo invoca el hilo del efecto cuando lo interrumpen. */
    public synchronized void efectoInterrumpido(String nombreEfecto) {
        hilosEfectos.remove(nombreEfecto, Thread.currentThread());
        normalizarEstado();
    }

    /**
     * Entrega los mensajes que generaron los hilos de efectos desde la ultima llamada.
     * Ya no aplica ticks: eso lo hacen los hilos.
     */
    public List<String> procesarEfectos() {
        List<String> mensajes = new ArrayList<>();
        String mensaje;
        while ((mensaje = mensajesPendientes.poll()) != null) {
            mensajes.add(mensaje);
        }
        return mensajes;
    }

    private String aplicarTick(EstadoEfecto efecto) {
        if (efecto.esDanio()) {
            recibirDanio(efecto.getValorPorTick());
            return String.format("%s sufre %d de dano por %s", nombre, efecto.getValorPorTick(), efecto.getNombre());
        }
        int curado = curar(efecto.getValorPorTick());
        return String.format("%s recupera %d de vida por %s", nombre, curado, efecto.getNombre());
    }

    private void cancelarHilo(String nombreEfecto) {
        Thread hilo = hilosEfectos.remove(nombreEfecto);
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    private void cancelarTodosLosHilos() {
        for (Thread hilo : hilosEfectos.values()) {
            hilo.interrupt();
        }
        hilosEfectos.clear();
    }

    private boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
            cancelarTodosLosHilos(); // un muerto ya no sufre ni recibe efectos
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    /** Copia de los efectos activos: iterar la vista viva podria lanzar ConcurrentModificationException. */
    public synchronized Collection<EstadoEfecto> getEfectos() {
        return List.copyOf(efectos.values());
    }
    // ---- serializacion

    /** synchronized: el auto-guardado no puede leer la vida a mitad de un tick de un hilo de efectos. */
    private synchronized void writeObject(ObjectOutputStream salida) throws IOException {
        salida.defaultWriteObject();
    }

    private void readObject(ObjectInputStream entrada) throws IOException, ClassNotFoundException {
        entrada.defaultReadObject();
        // Los campos transient no se restauran ni pasan por el inicializador: hay que recrearlos
        hilosEfectos = new HashMap<>();
        mensajesPendientes = new ConcurrentLinkedQueue<>();
        // Los hilos de efectos no se guardan, asi que los efectos activos tampoco se restauran
        efectos.clear();
        if (estado == EstadoPersonaje.ENVENENADO) {
            estado = EstadoPersonaje.VIVO;
        }
    }
}