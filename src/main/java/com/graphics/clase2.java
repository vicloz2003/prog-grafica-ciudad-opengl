package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected static final float INICIO_X = centro(0); // Salida en la calle del borde oeste.
    protected static final float INICIO_Z = centro(MAPA.length - 1); // Salida en la calle del borde sur.
    protected float autoX = INICIO_X; // Posición horizontal inicial: esquina suroeste.
    protected float autoZ = INICIO_Z; // Posición inicial en profundidad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float RADIO_AUTO = 1.9f; // Radio que contiene a la camioneta (incluida la rueda de repuesto).

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = INICIO_X; // Recupera la coordenada X de inicio.
        autoZ = INICIO_Z; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = 0.7f; // Define la pérdida de velocidad normal al rodar.

        if (pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        angulo += direccion * velocidad * 0.11f * deltaTime; // Gira según la velocidad; en reversa invierte el giro.
        float frenteX = -(float) Math.sin(angulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(angulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        if (puedeCircular(siguienteX, siguienteZ)) { // Comprueba la posición antes de mover el auto.
            autoX = siguienteX; // Acepta el desplazamiento horizontal.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else { // La posición propuesta invadiría una manzana o saldría del mapa.
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }

        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara | R: reiniciar | "; // Añade los controles de cámara y reinicio.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */
    protected boolean puedeCircular(float x, float z) {
        float limitePermitido = LIMITE - RADIO_AUTO; // Reserva espacio para que el auto completo quede dentro.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del auto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < RADIO_AUTO * RADIO_AUTO) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    // ==================== 5. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca la cámara 12 unidades detrás en Z.
        vector("uOjo", camaraX, 9, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        vector("uObjetivo", autoX, 1.1f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /**
     * Construye una camioneta todoterreno negra de líneas rectas.
     * Coordenadas locales: X = lado derecho, Y = altura, Z = hacia atrás (el frente mira a -Z).
     * Argumentos de pieza(): posición XYZ, tamaño XYZ y color RGB.
     */
    protected void dibujarAuto() {
        float negro = 0.07f; // Negro con un poco de valor para que la luz marque las caras.
        float[] gris = {0.20f, 0.20f, 0.22f}; // Parachoques, parrilla y estribos.
        float[] vidrio = {0.10f, 0.13f, 0.17f}; // Vidrios polarizados.

        // Ruedas grandes de todoterreno.
        float[] ladosRuedas = {-0.92f, 0.92f}; // Izquierda y derecha.
        float[] ejesRuedas = {-1.0f, 1.0f}; // Eje delantero y trasero.
        for (float x : ladosRuedas) {
            for (float z : ejesRuedas) {
                pieza(x, 0.4f, z, 0.34f, 0.8f, 0.8f, 0.04f, 0.04f, 0.05f); // Neumático.
                pieza(x * 1.1f, 0.4f, z, 0.04f, 0.36f, 0.36f, 0.45f, 0.46f, 0.48f); // Llanta vista desde el costado.
            }
        }

        // Carrocería: cuerpo inferior recto y cabina alta desplazada hacia atrás.
        pieza(0, 0.95f, 0, 1.8f, 0.75f, 3.1f, negro, negro, negro + 0.01f); // Cuerpo con el capó plano.
        pieza(0, 1.68f, 0.35f, 1.72f, 0.72f, 1.9f, negro, negro, negro + 0.01f); // Cabina.
        pieza(0, 0.55f, -1.62f, 1.9f, 0.3f, 0.16f, gris[0], gris[1], gris[2]); // Parachoques delantero.
        pieza(0, 0.55f, 1.62f, 1.9f, 0.3f, 0.16f, gris[0], gris[1], gris[2]); // Parachoques trasero.
        for (float x : ladosRuedas) {
            pieza(x * 1.03f, 0.45f, 0, 0.12f, 0.06f, 1.4f, gris[0], gris[1], gris[2]); // Estribo lateral.
            pieza(x * 1.07f, 1.5f, -0.5f, 0.12f, 0.14f, 0.2f, negro, negro, negro); // Espejo retrovisor.
        }

        // Vidrios de la cabina.
        pieza(0, 1.72f, -0.62f, 1.5f, 0.52f, 0.04f, vidrio[0], vidrio[1], vidrio[2]); // Parabrisas.
        pieza(0, 1.72f, 1.31f, 1.4f, 0.45f, 0.04f, vidrio[0], vidrio[1], vidrio[2]); // Luneta trasera.
        for (float x : ladosRuedas) {
            pieza(x * 0.94f, 1.72f, 0.35f, 0.04f, 0.5f, 1.6f, vidrio[0], vidrio[1], vidrio[2]); // Ventanas laterales.
        }

        // Frente: parrilla con listones, faros e intermitentes sobre los guardabarros.
        pieza(0, 0.98f, -1.56f, 1.0f, 0.42f, 0.04f, gris[0], gris[1], gris[2]); // Parrilla.
        for (float y = 0.88f; y <= 1.1f; y += 0.1f) {
            pieza(0, y, -1.585f, 0.95f, 0.03f, 0.02f, 0.45f, 0.46f, 0.48f); // Listón horizontal.
        }
        for (float x : new float[] {-0.64f, 0.64f}) {
            pieza(x, 1.0f, -1.57f, 0.28f, 0.28f, 0.05f, 1, 0.95f, 0.70f); // Faro delantero.
            pieza(x * 1.22f, 1.36f, -1.42f, 0.18f, 0.08f, 0.14f, 1, 0.55f, 0.10f); // Intermitente naranja.
            pieza(x * 1.25f, 1.0f, 1.57f, 0.14f, 0.32f, 0.05f, 0.85f, 0.05f, 0.05f); // Luz trasera roja.
        }

        // Rueda de repuesto en la puerta trasera y portaequipaje en el techo.
        pieza(0, 1.15f, 1.69f, 0.72f, 0.72f, 0.26f, 0.04f, 0.04f, 0.05f); // Rueda de repuesto.
        pieza(0, 1.15f, 1.83f, 0.3f, 0.3f, 0.04f, 0.45f, 0.46f, 0.48f); // Tapa central de la rueda.
        for (float x : new float[] {-0.7f, 0.7f}) {
            pieza(x, 2.09f, 0.35f, 0.06f, 0.06f, 1.7f, gris[0], gris[1], gris[2]); // Riel del portaequipaje.
        }
        for (float z : new float[] {-0.3f, 1.0f}) {
            pieza(0, 2.09f, z, 1.46f, 0.05f, 0.06f, gris[0], gris[1], gris[2]); // Barra transversal.
        }
    }

    /** Transforma una pieza del espacio local del auto del jugador al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        pieza(autoX, autoZ, angulo, x, y, z, sx, sy, sz, r, g, b); // Usa la posición y orientación del jugador.
    }

    /** Transforma una pieza local de cualquier vehículo, dada su posición y orientación en la ciudad. */
    protected void pieza(float baseX, float baseZ, float baseAngulo,
            float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(baseAngulo); // Calcula el coseno de la orientación del vehículo.
        float seno = (float) Math.sin(baseAngulo); // Calcula el seno de la misma orientación.
        float mundoX = baseX + coseno * x + seno * z; // Gira la posición local y suma la posición X del vehículo.
        float mundoZ = baseZ - seno * x + coseno * z; // Gira la posición local y suma la posición Z del vehículo.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, baseAngulo); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
