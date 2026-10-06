package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    protected float reloj = 0; // Reloj que nunca se detiene; anima semáforos y otros detalles.
    protected static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle.
        {-40, -30}, // Primera entrega: distrito comercial (noroeste).
        {40, -50}, // Segunda entrega: distrito financiero (noreste).
        {30, 40}, // Tercera entrega: distrito industrial (sureste).
        {-20, 50} // Cuarta entrega: regreso al distrito residencial (suroeste).
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        reloj += deltaTime; // Avanza siempre, también después de ganar.
        if (entregas >= DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de clase3.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | "; // Muestra la tecla que alterna el minimapa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar todas las paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: jugar otra vez"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/" + DESTINOS.length; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en la marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        if (vistaMapa) { // En el minimapa la marca va por encima de las torres (antenas ~36) y es más grande.
            caja(x, 38, z, 7, 0.1f, 7, 1, 0.72f, 0.12f); // Marca dorada visible desde arriba.
        } else {
            caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempo); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == PARQUE) { // Detecta una parcela de parque.
                    dibujarParque(x, z); // Añade árboles y un banco.
                }
                if (tipo != CALLE) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    dibujarSemaforo(x + 4, z - 4); // Coloca el semáforo dentro de la acera.
                }
            }
        }
        dibujarPasosPeatonales(); // Pinta los cruces de peatones en las intersecciones.
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /**
     * Pinta pasos peatonales en los cruces de las dos avenidas centrales (fila y columna del medio),
     * en cada entrada de la intersección. Solo allí, para no recargar la ciudad de franjas.
     * Cada paso queda justo antes del cruce.
     * Cada paso es una sola caja delgada que atraviesa la calle; el shader dibuja sus franjas
     * (material MAT_PASO), así se evitan cientos de cajas sueltas.
     */
    private void dibujarPasosPeatonales() {
        float separacion = CELDA / 2 + 1.1f; // Del centro del cruce al centro del paso: borde del cruce más medio paso.
        float ancho = CELDA - 1; // Cubre la calzada sin tocar las aceras.
        float fondo = 1.8f; // Ancho de la franja peatonal en el sentido de los autos.
        material(MAT_PASO);
        int avenida = MAPA.length / 2; // Índice de la avenida central (6 en un mapa de 13).
        for (int fila = 2; fila < MAPA.length - 1; fila += 2) { // Cruces interiores: filas y columnas pares sin los bordes.
            for (int columna = 2; columna < MAPA.length - 1; columna += 2) {
                if (fila != avenida && columna != avenida) { // Solo los cruces que están sobre una avenida central.
                    continue;
                }
                float x = centro(columna);
                float z = centro(fila);
                if (fila > 0) { // Entrada norte: el paso cruza la calle que llega desde el norte.
                    caja(x, 0.03f, z - separacion, ancho, 0.02f, fondo, 0.85f, 0.87f, 0.83f);
                }
                if (fila < MAPA.length - 1) { // Entrada sur.
                    caja(x, 0.03f, z + separacion, ancho, 0.02f, fondo, 0.85f, 0.87f, 0.83f);
                }
                if (columna > 0) { // Entrada oeste: el paso queda girado, a lo largo de Z.
                    caja(x - separacion, 0.03f, z, fondo, 0.02f, ancho, 0.85f, 0.87f, 0.83f);
                }
                if (columna < MAPA.length - 1) { // Entrada este.
                    caja(x + separacion, 0.03f, z, fondo, 0.02f, ancho, 0.85f, 0.87f, 0.83f);
                }
            }
        }
        material(MAT_PLANO);
    }

    /** Construye un semáforo decorativo que alterna rojo, verde y amarillo cada 12 segundos. */
    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las tres luces.
        int fase = (int) (reloj % 12); // Repite un ciclo de segundos comprendidos entre 0 y 11.
        for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
            boolean encendida = false; // Parte de una bombilla apagada.
            if (indice == 0) { // Selecciona la bombilla roja.
                encendida = fase < 5; // Mantiene rojo durante los primeros cinco segundos.
            } else if (indice == 1) { // Selecciona la bombilla amarilla.
                encendida = fase >= 10; // Mantiene amarillo en los últimos dos segundos del ciclo.
            } else { // Selecciona la bombilla verde.
                encendida = fase >= 5 && fase < 10; // Mantiene verde durante los cinco segundos intermedios.
            }
            float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
            entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
            if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                brillo = 1; // Usa intensidad completa para su color.
                entero("uEmision", 1); // Hace que la bombilla se vea encendida.
            }
            float rojo = 0; // Componente roja inicialmente ausente.
            float verde = 0; // Componente verde inicialmente ausente.
            if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                rojo = brillo; // Añade rojo con la intensidad elegida.
            }
            if (indice > 0) { // Amarillo y verde necesitan componente verde.
                verde = brillo; // Añade verde; rojo más verde produce amarillo.
            }
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Dibuja la bombilla frente a la carcasa.
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (!mostrarMapa) { // Comprueba si el usuario ocultó el minimapa con M.
            return; // Conserva únicamente la imagen principal.
        }
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 40, autoZ, 4, 0.1f, 5.6f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 3.5f; // Desplaza la punta 3.5 unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 3.5f; // Desplaza la punta 3.5 unidades hacia delante en Z.
        caja(puntaX, 41, puntaZ, 1.6f, 0.1f, 1.6f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
