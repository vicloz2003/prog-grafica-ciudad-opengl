package com.graphics; // Última etapa del mismo proyecto.

import java.util.ArrayList; // Lista de vehículos del tráfico.
import java.util.List; // Tipo de la lista.
import java.util.Random; // Elige giros en los cruces.

/**
 * CLASE 5: TRÁFICO AUTÓNOMO.
 * Hereda la ciudad completa de clase4 y agrega cuatro vehículos que circulan solos.
 * Cada vehículo va de cruce en cruce por el carril derecho; al llegar a un cruce elige
 * una nueva dirección válida sin dar media vuelta. Nunca entra en una manzana porque
 * solo se desplaza por los ejes de las calles.
 * Orden de lectura: modelos y estado, creación, reglas de movimiento, colisiones y dibujo.
 */
public class clase5 extends clase4 {

    // ==================== 1. MODELOS Y ESTADO ====================
    protected static final int TAXI = 0; // Auto amarillo con letrero en el techo.
    protected static final int AUTOBUS = 1; // Vehículo largo de transporte público.
    protected static final int FURGONETA = 2; // Furgoneta de reparto con caja alta.
    protected static final int PATRULLA = 3; // Auto de policía con barra de luces.

    protected static final float CARRIL = 2.5f; // Distancia del eje de la calle al centro del carril derecho.
    private static final float GIRO_MAXIMO = 4; // Radianes por segundo que puede girar la carrocería.
    private static final float ACELERACION_TRAFICO = 5; // Cambio de velocidad por segundo.
    private static final float DISTANCIA_SEGURIDAD = 8; // Distancia a la que frena si hay algo delante.

    /** Estado de un vehículo del tráfico. */
    protected static class Vehiculo {
        final int modelo; // Tipo de vehículo, decide su forma y colores.
        final float crucero; // Velocidad normal de circulación.
        final float radio; // Radio de colisión usado contra el jugador.
        float x; // Posición X en la ciudad.
        float z; // Posición Z en la ciudad.
        float angulo; // Orientación de la carrocería; cero mira hacia -Z.
        float velocidad; // Velocidad actual en unidades por segundo.
        int cruceFila; // Fila (par) del cruce hacia el que se dirige.
        int cruceColumna; // Columna (par) del cruce hacia el que se dirige.
        int pasoFila; // Dirección actual en filas: -1 norte, 1 sur, 0 sin movimiento vertical.
        int pasoColumna; // Dirección actual en columnas: -1 oeste, 1 este.

        Vehiculo(int modelo, float crucero, float radio) {
            this.modelo = modelo;
            this.crucero = crucero;
            this.radio = radio;
        }
    }

    protected final List<Vehiculo> trafico = new ArrayList<>(); // Todos los vehículos autónomos.
    private final Random azar = new Random(7); // Semilla fija: el recorrido se repite igual en cada ejecución.

    /** Crea el tráfico al construir la aplicación (no necesita OpenGL). */
    public clase5() {
        crearTrafico();
    }

    // ==================== 2. CREACIÓN DEL TRÁFICO ====================

    /** Coloca cada vehículo en un cruce distinto, mirando hacia un cruce vecino. */
    protected void crearTrafico() {
        trafico.clear();
        int ultimo = MAPA.length - 1; // Índice del borde sur y del borde este.
        agregar(new Vehiculo(TAXI, 9, 1.8f), 6, 0, 0, 1); // Avenida central, hacia el este.
        agregar(new Vehiculo(AUTOBUS, 6, 3.2f), 0, 6, 1, 0); // Borde norte, hacia el sur.
        agregar(new Vehiculo(FURGONETA, 7, 2.2f), ultimo, 8, -1, 0); // Borde sur, hacia el norte.
        agregar(new Vehiculo(PATRULLA, 10, 1.8f), 6, ultimo, 0, -1); // Borde este, hacia el oeste.
    }

    /** Ubica un vehículo en el cruce (fila, columna) apuntando en la dirección indicada. */
    private void agregar(Vehiculo v, int fila, int columna, int pasoFila, int pasoColumna) {
        v.pasoFila = pasoFila;
        v.pasoColumna = pasoColumna;
        float derechaX = -pasoFila; // Vector derecho de la dirección (pasoColumna, pasoFila).
        float derechaZ = pasoColumna;
        v.x = centro(columna) + derechaX * CARRIL; // Empieza en su carril derecho.
        v.z = centro(fila) + derechaZ * CARRIL;
        v.angulo = anguloHacia(pasoColumna, pasoFila); // La carrocería ya mira hacia donde va.
        v.velocidad = v.crucero;
        v.cruceFila = fila + 2 * pasoFila; // El primer destino es el cruce vecino.
        v.cruceColumna = columna + 2 * pasoColumna;
        trafico.add(v);
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Mueve al jugador (clases anteriores) y después al tráfico. */
    @Override
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Conducción, entregas y reloj heredados.
        actualizarTrafico(deltaTime); // Tráfico autónomo.
    }

    /** Avanza todos los vehículos; no usa GLFW, por eso se puede probar sin ventana. */
    protected void actualizarTrafico(float deltaTime) {
        for (Vehiculo v : trafico) {
            float objetivo = hayObstaculoDelante(v) ? 0 : v.crucero; // Frena si algo bloquea su carril.
            float cambio = ACELERACION_TRAFICO * deltaTime; // Cuánto puede variar la velocidad en este cuadro.
            v.velocidad += Math.max(-cambio, Math.min(cambio, objetivo - v.velocidad)); // Acerca la velocidad al objetivo.

            float destinoX = puntoCarrilX(v); // Punto del carril derecho en el próximo cruce.
            float destinoZ = puntoCarrilZ(v);
            float dx = destinoX - v.x;
            float dz = destinoZ - v.z;
            float distancia = (float) Math.sqrt(dx * dx + dz * dz);
            float avance = v.velocidad * deltaTime; // Distancia que recorre en este cuadro.

            if (avance >= distancia) { // Llega al cruce en este cuadro.
                v.x = destinoX;
                v.z = destinoZ;
                elegirSiguienteCruce(v); // Decide hacia dónde seguir.
            } else if (distancia > 0) { // Avanza en línea recta hacia el cruce.
                v.x += dx / distancia * avance;
                v.z += dz / distancia * avance;
            }

            if (distancia > 0.01f) { // Gira la carrocería suavemente hacia la dirección de avance.
                float deseado = anguloHacia(dx, dz);
                float diferencia = normalizar(deseado - v.angulo);
                float maximo = GIRO_MAXIMO * deltaTime;
                v.angulo = normalizar(v.angulo + Math.max(-maximo, Math.min(maximo, diferencia)));
            }
        }
    }

    /** Punto X del carril derecho en el cruce destino, según la dirección actual. */
    private float puntoCarrilX(Vehiculo v) {
        return centro(v.cruceColumna) - v.pasoFila * CARRIL; // Derecha de (pasoColumna, pasoFila) = (-pasoFila, pasoColumna).
    }

    /** Punto Z del carril derecho en el cruce destino, según la dirección actual. */
    private float puntoCarrilZ(Vehiculo v) {
        return centro(v.cruceFila) + v.pasoColumna * CARRIL;
    }

    /** En un cruce, elige al azar una dirección que no salga del mapa ni dé media vuelta. */
    private void elegirSiguienteCruce(Vehiculo v) {
        int[][] direcciones = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Norte, sur, oeste, este (fila, columna).
        List<int[]> opciones = new ArrayList<>();
        for (int[] d : direcciones) {
            boolean mediaVuelta = d[0] == -v.pasoFila && d[1] == -v.pasoColumna;
            int fila = v.cruceFila + 2 * d[0]; // Los cruces están en índices pares, cada dos celdas.
            int columna = v.cruceColumna + 2 * d[1];
            boolean dentro = fila >= 0 && fila < MAPA.length && columna >= 0 && columna < MAPA.length;
            if (dentro && !mediaVuelta) {
                opciones.add(d);
            }
        }
        int[] elegida = opciones.get(azar.nextInt(opciones.size())); // En la cuadrícula siempre hay al menos dos opciones.
        v.pasoFila = elegida[0];
        v.pasoColumna = elegida[1];
        v.cruceFila += 2 * elegida[0];
        v.cruceColumna += 2 * elegida[1];
    }

    /** Comprueba si el jugador, o un vehículo que va en el mismo sentido, está justo delante. */
    private boolean hayObstaculoDelante(Vehiculo v) {
        float frenteX = -(float) Math.sin(v.angulo);
        float frenteZ = -(float) Math.cos(v.angulo);
        if (estaDelante(v, frenteX, frenteZ, autoX, autoZ)) { // El jugador siempre tiene prioridad.
            return true;
        }
        for (Vehiculo otro : trafico) {
            if (otro == v) {
                continue;
            }
            float otroFrenteX = -(float) Math.sin(otro.angulo);
            float otroFrenteZ = -(float) Math.cos(otro.angulo);
            boolean mismoSentido = frenteX * otroFrenteX + frenteZ * otroFrenteZ > 0.5f; // Solo hace fila; evita bloqueos en cruces.
            if (mismoSentido && estaDelante(v, frenteX, frenteZ, otro.x, otro.z)) {
                return true;
            }
        }
        return false;
    }

    /** Indica si el punto (px, pz) está en el corredor de frenado delante del vehículo. */
    private boolean estaDelante(Vehiculo v, float frenteX, float frenteZ, float px, float pz) {
        float haciaX = px - v.x;
        float haciaZ = pz - v.z;
        float adelante = haciaX * frenteX + haciaZ * frenteZ; // Proyección sobre la dirección de avance.
        float lateral = Math.abs(haciaX * frenteZ - haciaZ * frenteX); // Distancia al eje del vehículo.
        return adelante > 0 && adelante < DISTANCIA_SEGURIDAD + v.radio && lateral < 2.8f;
    }

    /** Ángulo de la carrocería para avanzar en la dirección (dx, dz); el frente local es -Z. */
    protected static float anguloHacia(float dx, float dz) {
        return (float) Math.atan2(-dx, -dz);
    }

    /** Lleva un ángulo al intervalo [-PI, PI] para girar por el lado más corto. */
    private static float normalizar(float angulo) {
        while (angulo > Math.PI) {
            angulo -= 2 * Math.PI;
        }
        while (angulo < -Math.PI) {
            angulo += 2 * Math.PI;
        }
        return angulo;
    }

    // ==================== 4. COLISIÓN DEL JUGADOR CON EL TRÁFICO ====================

    /** El jugador tampoco atraviesa a los vehículos, pero siempre puede alejarse de ellos. */
    @Override
    protected boolean puedeCircular(float x, float z) {
        if (!super.puedeCircular(x, z)) { // Manzanas y bordes de clase2.
            return false;
        }
        for (Vehiculo v : trafico) {
            float suma = RADIO_AUTO + v.radio;
            float nuevaX = x - v.x;
            float nuevaZ = z - v.z;
            float actualX = autoX - v.x;
            float actualZ = autoZ - v.z;
            float nueva = nuevaX * nuevaX + nuevaZ * nuevaZ;
            float actual = actualX * actualX + actualZ * actualZ;
            if (nueva < suma * suma && nueva < actual) { // Solo bloquea si el movimiento acerca los vehículos.
                return false;
            }
        }
        return true;
    }

    /** Al reiniciar también vuelve el tráfico a su posición inicial. */
    @Override
    protected void reiniciar() {
        super.reiniciar();
        crearTrafico();
    }

    // ==================== 5. DIBUJO DE LOS VEHÍCULOS ====================

    /** Añade el tráfico a la escena; aparece también en el minimapa porque escena() se redibuja allí. */
    @Override
    protected void escena() {
        super.escena();
        for (Vehiculo v : trafico) {
            if (v.modelo == TAXI) {
                dibujarTaxi(v);
            } else if (v.modelo == AUTOBUS) {
                dibujarAutobus(v);
            } else if (v.modelo == FURGONETA) {
                dibujarFurgoneta(v);
            } else {
                dibujarPatrulla(v);
            }
        }
    }

    /** Atajo: dibuja una pieza en el espacio local del vehículo v. */
    private void p(Vehiculo v, float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        pieza(v.x, v.z, v.angulo, x, y, z, sx, sy, sz, r, g, b);
    }

    /** Cuatro ruedas oscuras con la separación lateral y la distancia entre ejes indicadas. */
    private void ruedas(Vehiculo v, float lado, float eje, float tamano) {
        for (float x : new float[] {-lado, lado}) {
            for (float z : new float[] {-eje, eje}) {
                p(v, x, tamano / 2, z, 0.28f, tamano, tamano, 0.05f, 0.05f, 0.06f);
            }
        }
    }

    /** Faros delanteros y luces traseras en la cara frontal (z negativa) y trasera (z positiva). */
    private void luces(Vehiculo v, float lado, float y, float frente, float atras) {
        for (float x : new float[] {-lado, lado}) {
            p(v, x, y, frente, 0.3f, 0.18f, 0.05f, 1, 0.95f, 0.7f);
            p(v, x, y, atras, 0.3f, 0.16f, 0.05f, 0.85f, 0.05f, 0.05f);
        }
    }

    private void dibujarTaxi(Vehiculo v) {
        ruedas(v, 0.85f, 0.95f, 0.6f);
        p(v, 0, 0.62f, 0, 1.7f, 0.55f, 3.0f, 0.95f, 0.78f, 0.10f); // Carrocería amarilla.
        p(v, 0, 0.62f, 0, 1.72f, 0.12f, 2.9f, 0.08f, 0.08f, 0.08f); // Franja negra lateral.
        p(v, 0, 1.12f, 0.2f, 1.45f, 0.48f, 1.5f, 0.95f, 0.78f, 0.10f); // Cabina.
        p(v, 0, 1.12f, -0.56f, 1.3f, 0.38f, 0.04f, 0.12f, 0.16f, 0.2f); // Parabrisas.
        luces(v, 0.55f, 0.66f, -1.52f, 1.52f);
        if (noche) { // El letrero del techo se ve encendido de noche.
            entero("uEmision", 1);
        }
        p(v, 0, 1.47f, 0.2f, 0.6f, 0.2f, 0.28f, 1, 0.95f, 0.75f); // Letrero de taxi.
        entero("uEmision", 0);
    }

    private void dibujarAutobus(Vehiculo v) {
        ruedas(v, 0.95f, 2.1f, 0.8f);
        p(v, 0, 1.45f, 0, 2.1f, 2.2f, 6.0f, 0.15f, 0.55f, 0.35f); // Carrocería verde.
        p(v, 0, 1.95f, 0.2f, 2.12f, 0.6f, 5.2f, 0.10f, 0.13f, 0.17f); // Franja de ventanas.
        p(v, 0, 1.85f, -3.01f, 1.8f, 0.8f, 0.04f, 0.12f, 0.16f, 0.2f); // Parabrisas.
        p(v, 0, 2.6f, 0, 1.9f, 0.1f, 5.6f, 0.85f, 0.85f, 0.82f); // Techo claro.
        luces(v, 0.7f, 0.8f, -3.02f, 3.02f);
    }

    private void dibujarFurgoneta(Vehiculo v) {
        ruedas(v, 0.9f, 1.1f, 0.66f);
        p(v, 0, 1.05f, -1.25f, 1.8f, 1.4f, 1.3f, 0.92f, 0.92f, 0.90f); // Cabina blanca.
        p(v, 0, 1.4f, -1.91f, 1.5f, 0.55f, 0.04f, 0.12f, 0.16f, 0.2f); // Parabrisas.
        p(v, 0, 1.45f, 0.7f, 1.9f, 2.1f, 2.5f, 0.95f, 0.95f, 0.93f); // Caja de carga.
        p(v, 0, 1.2f, 0.7f, 1.92f, 0.3f, 2.3f, 0.85f, 0.35f, 0.10f); // Franja de la empresa de reparto.
        luces(v, 0.6f, 0.75f, -1.92f, 1.97f);
    }

    private void dibujarPatrulla(Vehiculo v) {
        ruedas(v, 0.85f, 0.95f, 0.6f);
        p(v, 0, 0.62f, 0, 1.7f, 0.55f, 3.0f, 0.92f, 0.92f, 0.92f); // Carrocería blanca.
        p(v, 0, 0.92f, -1.05f, 1.72f, 0.06f, 0.9f, 0.06f, 0.06f, 0.07f); // Capó negro.
        p(v, 0, 0.92f, 1.15f, 1.72f, 0.06f, 0.7f, 0.06f, 0.06f, 0.07f); // Maletero negro.
        p(v, 0, 1.12f, 0.1f, 1.45f, 0.48f, 1.4f, 0.06f, 0.06f, 0.07f); // Cabina negra.
        luces(v, 0.55f, 0.66f, -1.52f, 1.52f);
        boolean fase = ((int) (reloj * 4)) % 2 == 0; // Alterna cuatro veces por segundo.
        entero("uEmision", fase ? 1 : 0);
        p(v, -0.25f, 1.43f, 0.1f, 0.42f, 0.14f, 0.28f, fase ? 1 : 0.35f, 0.05f, 0.05f); // Luz roja.
        entero("uEmision", fase ? 0 : 1);
        p(v, 0.25f, 1.43f, 0.1f, 0.42f, 0.14f, 0.28f, 0.05f, 0.15f, fase ? 0.35f : 1); // Luz azul.
        entero("uEmision", 0);
    }

    /** Punto de entrada de la versión final con tráfico. */
    public static void main(String[] args) {
        clase5 aplicacion = new clase5(); // Crea la ciudad completa con tráfico autónomo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
