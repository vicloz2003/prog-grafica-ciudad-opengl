package com.graphics; // Coloca la aplicación en el paquete usado por los ejemplos originales.

import java.util.HashMap; // Permite guardar las ubicaciones de las variables del shader.
import java.util.Map; // Define el tipo del diccionario de ubicaciones.
import org.lwjgl.glfw.Callbacks; // Permite liberar los callbacks de la ventana al cerrar.
import org.lwjgl.glfw.GLFWErrorCallback; // Muestra los errores de GLFW en la consola.
import org.lwjgl.opengl.GL; // Carga las funciones OpenGL disponibles en el equipo.
import static org.lwjgl.glfw.GLFW.*; // Importa las funciones y constantes de ventanas y teclado.
import static org.lwjgl.opengl.GL33.*; // Importa las funciones OpenGL hasta la versión 3.3.

/**
 * CLASE 1: CREAR LA CIUDAD.
 * Sigue el flujo de los ejemplos: iniciar, loop, dibujar y limpiar.
 * Coordenadas: X = izquierda/derecha; Y = altura; Z = profundidad.
 * Las clases siguientes reutilizan esta base mediante extends y super.
 * Los comentarios explican las instrucciones; las llaves solo delimitan bloques.
 */
public class clase1 {

    // ==================== 1. VARIABLES DE LA APLICACIÓN ====================
    protected long ventana; // Identificador que GLFW asigna a la ventana.
    protected int programa; // Identificador del programa que une ambos shaders.
    protected int vao; // Identificador del VAO: describe cómo leer los vértices.
    protected int vbo; // Identificador del VBO: contiene los vértices en la GPU.
    protected int ancho = 1100; // Ancho inicial de la ventana; después contiene píxeles del framebuffer.
    protected int alto = 760; // Alto inicial de la ventana; después contiene píxeles del framebuffer.
    protected float orbita = 0.6f; // Ángulo inicial de la cámara alrededor de la ciudad, en radianes.
    protected boolean vistaMapa = false; // Clase4 lo activa cuando dibuja la vista superior.
    private final Map<String, Integer> uniforms = new HashMap<>(); // Evita buscar repetidamente el mismo uniform.

    // Tipos de celda que puede contener el mapa.
    protected static final int CALLE = 0; // Celda transitable.
    protected static final int RESIDENCIAL = 1; // Edificio bajo de viviendas (distrito suroeste).
    protected static final int PARQUE = 2; // Parcela verde con árboles y banco.
    protected static final int FINANCIERO = 3; // Rascacielos de oficinas (distrito noreste).
    protected static final int COMERCIAL = 4; // Edificio mediano con tiendas (distrito noroeste).
    protected static final int INDUSTRIAL = 5; // Galpón con chimenea (distrito sureste).

    /*
     * Ciudad de 13 x 13 celdas. Filas y columnas pares son calles continuas;
     * las impares son manzanas. La fila 6 y la columna 6 forman las avenidas
     * centrales que dividen la ciudad en cuatro distritos:
     *   NO = comercial (4)   NE = financiero (3)
     *   SO = residencial (1) SE = industrial (5)     2 = parque
     * Las filas corresponden a Z (norte arriba = -Z) y las columnas a X.
     */
    protected static final int[][] MAPA = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Calle del borde norte.
        {0, 4, 0, 4, 0, 2, 0, 3, 0, 3, 0, 3, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 4, 0, 2, 0, 4, 0, 3, 0, 2, 0, 3, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 4, 0, 4, 0, 4, 0, 3, 0, 3, 0, 3, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Avenida central este-oeste.
        {0, 1, 0, 1, 0, 2, 0, 5, 0, 5, 0, 5, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 2, 0, 1, 0, 1, 0, 5, 0, 2, 0, 5, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0, 5, 0, 5, 0, 5, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0} // Calle del borde sur.
    };
    protected static final float CELDA = 10; // Ancho y profundidad de cada celda (igual que en el mapa original).
    protected static final float LIMITE = MAPA.length * CELDA / 2; // Distancia del origen a cada borde: 65 unidades.

    // ==================== 2. INICIO, CICLO Y LIMPIEZA ====================

    /** Organiza las tres fases de la aplicación y garantiza la limpieza si ocurre un error. */
    public void run() {
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err); // Prepara mensajes de error en consola.
        errores.set(); // Instala el manejador de errores de GLFW.
        try { // Protege el arranque y el ciclo para poder liberar recursos si fallan.
            iniciar(); // Crea la ventana, los shaders y la geometría.
            loop(); // Actualiza y dibuja hasta que se cierre la ventana.
        } finally { // Este bloque se ejecuta tanto al salir normalmente como al fallar.
            limpiar(); // Libera la GPU, los callbacks y la ventana.
            glfwSetErrorCallback(null); // Desconecta el manejador antes de liberar su memoria.
            errores.free(); // Libera el callback de errores.
        }
    }

    /** Configura GLFW y OpenGL, igual que las clases de cámara del proyecto original. */
    private void iniciar() {
        if (!glfwInit()) { // Intenta iniciar la biblioteca que administra ventanas y entrada.
            throw new IllegalStateException("No se pudo iniciar GLFW"); // Detiene el arranque si GLFW falla.
        }
        glfwDefaultWindowHints(); // Restablece las opciones de ventana antes de personalizarlas.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3); // Solicita la versión mayor de OpenGL.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3); // Solicita OpenGL 3.3.
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Usa el perfil moderno basado en shaders.
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Solicita compatibilidad con contextos modernos de macOS.
        ventana = glfwCreateWindow(ancho, alto, "Clase 1 | Ciudad | Flechas: orbitar | ESC: salir", 0, 0); // Crea una ventana normal.
        if (ventana == 0) { // GLFW devuelve cero cuando no consigue crear la ventana.
            throw new IllegalStateException("No se pudo crear la ventana OpenGL"); // Informa el fallo de creación.
        }
        glfwMakeContextCurrent(ventana); // Selecciona la ventana que recibirá las llamadas OpenGL.
        glfwSwapInterval(1); // Sincroniza la presentación con la pantalla para reducir cortes de imagen.
        GL.createCapabilities(); // Carga las funciones OpenGL del contexto actual.
        glEnable(GL_DEPTH_TEST); // Hace que las superficies cercanas oculten las lejanas.
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> { // Registra la función que recibe eventos de teclado.
            if (action == GLFW_PRESS) { // Responde una sola vez al presionar, no a las repeticiones de la tecla.
                tecla(key); // Entrega la tecla al método que cada lección puede ampliar.
            }
        }); // Termina el registro del callback de teclado.
        crearPrograma(); // Compila y enlaza los shaders que transforman y colorean los vértices.
        crearCubo(); // Guarda en la GPU el cubo que servirá para todos los objetos.
    }

    /** Mantiene el ciclo continuo de eventos, actualización, dibujo y presentación. */
    private void loop() {
        double tiempoAnterior = glfwGetTime(); // Guarda el instante inicial para calcular el tiempo entre cuadros.
        int cuadros = 0; // Cuenta los cuadros dibujados para la comprobación automática opcional.
        int maxCuadros = Integer.getInteger("demo.frames", 0); // Cero permite jugar; otro valor limita el arranque de prueba.
        int[] anchoReal = new int[1]; // Reserva espacio para que GLFW escriba el ancho en píxeles.
        int[] altoReal = new int[1]; // Reserva espacio para que GLFW escriba el alto en píxeles.

        while (!glfwWindowShouldClose(ventana)) { // Repite mientras no se haya solicitado cerrar.
            glfwPollEvents(); // Procesa teclado, redimensionamiento y botón de cierre.
            double tiempoActual = glfwGetTime(); // Consulta el tiempo actual en segundos.
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05); // Evita saltos de más de 50 ms.
            tiempoAnterior = tiempoActual; // Conserva el instante de este cuadro para la próxima vuelta.
            actualizar(deltaTime); // Actualiza cámara o conducción según la etapa ejecutada.
            glfwGetFramebufferSize(ventana, anchoReal, altoReal); // Obtiene los píxeles reales, también en pantallas Retina.
            ancho = anchoReal[0]; // Copia el ancho actual al estado de la aplicación.
            alto = altoReal[0]; // Copia el alto actual al estado de la aplicación.
            if (ancho > 0 && alto > 0) { // Evita dibujar y dividir por cero cuando la ventana está minimizada.
                dibujarFrame(); // Genera la imagen de la ciudad para este cuadro.
            }
            glfwSwapBuffers(ventana); // Muestra la imagen terminada intercambiando los buffers.
            cuadros++; // Registra que se completó una vuelta del ciclo.
            if (maxCuadros > 0 && cuadros >= maxCuadros) { // Detecta el final de un arranque automático limitado.
                glfwSetWindowShouldClose(ventana, true); // Solicita salir por el mismo camino que un cierre normal.
            }
        }
    }

    /** Libera únicamente los recursos que llegaron a crearse. */
    private void limpiar() {
        if (programa != 0) { // Comprueba si existe un programa en la GPU.
            glDeleteProgram(programa); // Libera el programa de shaders.
        }
        if (vbo != 0) { // Comprueba si se reservó el buffer de vértices.
            glDeleteBuffers(vbo); // Libera los datos de geometría de la GPU.
        }
        if (vao != 0) { // Comprueba si se creó la descripción de atributos.
            glDeleteVertexArrays(vao); // Libera la configuración del cubo.
        }
        if (ventana != 0) { // Evita destruir una ventana que no pudo crearse.
            Callbacks.glfwFreeCallbacks(ventana); // Libera el callback de teclado registrado.
            glfwDestroyWindow(ventana); // Destruye la ventana y su contexto OpenGL.
        }
        glfwTerminate(); // Finaliza GLFW y sus recursos globales.
    }

    // ==================== 3. TECLADO Y CÁMARA ====================

    /** Atiende las acciones de una sola pulsación. */
    protected void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) { // Comprueba si se presionó ESC.
            glfwSetWindowShouldClose(ventana, true); // Solicita terminar el ciclo principal.
        }
    }

    /** Consulta si una tecla sigue presionada durante el cuadro actual. */
    protected boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS; // Devuelve true mientras el usuario mantiene la tecla.
    }

    /** Mueve la cámara alrededor de la ciudad con las flechas horizontales. */
    protected void actualizar(float deltaTime) {
        if (pulsada(GLFW_KEY_LEFT)) { // Comprueba la flecha izquierda.
            orbita -= deltaTime; // Reduce el ángulo a razón de un radián por segundo.
        }
        if (pulsada(GLFW_KEY_RIGHT)) { // Comprueba la flecha derecha.
            orbita += deltaTime; // Aumenta el ángulo a la misma velocidad.
        }
    }

    /** Coloca la cámara elevada y orientada hacia el centro de la ciudad. */
    protected void configurarCamara() {
        float radio = LIMITE * 1.85f; // La órbita crece con el tamaño del mapa para verlo completo.
        float altura = LIMITE * 1.55f; // La altura también depende del mapa.
        float camaraX = (float) Math.sin(orbita) * radio; // Calcula la posición X de la órbita.
        float camaraZ = (float) Math.cos(orbita) * radio; // Calcula la posición Z de esa misma órbita.
        vector("uOjo", camaraX, altura, camaraZ); // Envía la posición elevada de la cámara.
        vector("uObjetivo", 0, 0, 0); // Apunta la cámara hacia el origen del mundo.
        decimal("uAspecto", (float) ancho / alto); // Envía la proporción de la imagen para evitar deformaciones.
    }

    /** Reserva el punto en el que clase3 enviará las luces a la GPU. */
    protected void prepararLuces() {
        // No necesita instrucciones en clase1: el shader inicial utiliza colores planos.
    }

    // ==================== 4. CIUDAD A PARTIR DE UNA MATRIZ ====================

    /** Convierte el índice de una fila o columna al centro de su celda. */
    protected static float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f); // Parte del borde negativo y avanza hasta la mitad de la celda.
    }

    /** Prepara OpenGL y dibuja una imagen completa. */
    protected void dibujarFrame() {
        glViewport(0, 0, ancho, alto); // Utiliza toda el área de la ventana para la escena principal.
        glClearColor(0.12f, 0.20f, 0.30f, 1); // Define un fondo azul oscuro completamente opaco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra la imagen y las distancias del cuadro anterior.
        glUseProgram(programa); // Activa los shaders de esta etapa.
        glBindVertexArray(vao); // Selecciona los atributos del cubo compartido.
        entero("uMapa", 0); // Selecciona perspectiva normal, no la proyección del minimapa.
        decimal("uMedioMapa", LIMITE + 2); // Mitad del área que abarca el minimapa, con un pequeño margen.
        configurarCamara(); // Actualiza la posición y el objetivo de la cámara.
        prepararLuces(); // Envía iluminación si la etapa actual la implementa.
        escena(); // Dibuja la ciudad y las ampliaciones de la lección actual.
        if (glGetError() != GL_NO_ERROR) { // Comprueba si OpenGL reportó una operación inválida.
            throw new IllegalStateException("Error OpenGL al dibujar"); // Hace visible el problema en consola.
        }
    }

    /** Recorre el mapa y transforma cada celda en geometría. */
    protected void escena() {
        float lado = 2 * LIMITE; // El suelo cubre todo el mapa ampliado.
        caja(0, -0.25f, 0, lado, 0.5f, lado, 0.16f, 0.19f, 0.23f); // Dibuja la base de asfalto con su cara superior en Y=0.
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre el mapa de norte a sur.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre cada fila de izquierda a derecha.
                float x = centro(columna); // Convierte la columna a posición X.
                float z = centro(fila); // Convierte la fila a posición Z.
                int tipo = MAPA[fila][columna]; // Lee el tipo de celda.
                if (tipo == CALLE) { // Selecciona las celdas transitables.
                    dibujarMarcasCalle(fila, columna, x, z); // Añade líneas amarillas entre intersecciones.
                } else { // Las demás celdas representan manzanas completas.
                    caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f); // Dibuja la acera elevada sobre el asfalto.
                    if (tipo == PARQUE) { // Un parque solo lleva césped; clase4 añade árboles y banco.
                        caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f); // Cubre la parcela con césped verde.
                    } else { // Cualquier otro tipo es un edificio de algún distrito.
                        dibujarEdificio(tipo, fila, columna, x, z); // Construye el edificio según su distrito.
                    }
                }
            }
        }
    }

    // ==================== 4b. EDIFICIOS POR DISTRITO ====================

    /** Número reproducible entre 0 y 8 que varía de una manzana a otra. */
    protected static int variacion(int fila, int columna) {
        return (fila * 3 + columna * 7) % 9; // Misma fórmula del mapa original.
    }

    /** Altura del cuerpo del edificio según su distrito. */
    protected static float alturaEdificio(int tipo, int fila, int columna) {
        int v = variacion(fila, columna); // Obtiene la variación de esta manzana.
        if (tipo == FINANCIERO) { // Rascacielos: entre 18 y 30.
            return 18 + (v * 3 + fila) % 13;
        }
        if (tipo == COMERCIAL) { // Edificios medianos: entre 6 y 12.
            return 6 + (v * 5) % 7;
        }
        if (tipo == INDUSTRIAL) { // Galpones bajos: entre 3 y 5.
            return 3 + v % 3;
        }
        return 4 + v % 5; // Residencial: entre 4 y 8.
    }

    /** Ancho (y profundidad) del edificio; los galpones ocupan más superficie. */
    protected static float anchoEdificio(int tipo) {
        if (tipo == INDUSTRIAL) { // El galpón casi llena la acera.
            return 8.4f;
        }
        if (tipo == FINANCIERO) { // La torre es un poco más esbelta.
            return 6.6f;
        }
        return 7; // Residencial y comercial conservan el ancho original.
    }

    /** Dibuja el cuerpo, la cubierta y el detalle característico de cada distrito. */
    protected void dibujarEdificio(int tipo, int fila, int columna, float x, float z) {
        float altura = alturaEdificio(tipo, fila, columna); // Altura del cuerpo principal.
        float ancho = anchoEdificio(tipo); // Tamaño de la planta.
        int v = variacion(fila, columna); // Variación para alternar colores.
        float base = 0.3f; // El edificio se apoya sobre la acera.
        float r;
        float g;
        float b;
        if (tipo == FINANCIERO) { // Vidrio azul acero o gris claro.
            boolean azul = v % 2 == 0;
            r = azul ? 0.30f : 0.52f;
            g = azul ? 0.44f : 0.56f;
            b = azul ? 0.60f : 0.62f;
        } else if (tipo == COMERCIAL) { // Paleta de colores vivos.
            float[][] paleta = {{0.82f, 0.30f, 0.28f}, {0.93f, 0.63f, 0.20f}, {0.28f, 0.58f, 0.84f}, {0.55f, 0.36f, 0.74f}};
            float[] color = paleta[v % paleta.length];
            r = color[0];
            g = color[1];
            b = color[2];
        } else if (tipo == INDUSTRIAL) { // Gris de hormigón u óxido.
            boolean oxido = v % 2 == 1;
            r = oxido ? 0.55f : 0.47f;
            g = oxido ? 0.36f : 0.47f;
            b = oxido ? 0.26f : 0.45f;
        } else { // Residencial: terracota o crema.
            boolean terracota = v % 2 == 0;
            r = terracota ? 0.72f : 0.86f;
            g = terracota ? 0.40f : 0.78f;
            b = terracota ? 0.28f : 0.60f;
        }
        caja(x, base + altura / 2, z, ancho, altura, ancho, r, g, b); // Cuerpo principal del edificio.
        float techo = base + altura; // Altura de la cara superior del cuerpo.

        if (tipo == FINANCIERO) { // Torre: remate escalonado y antena.
            caja(x, techo + 0.6f, z, ancho * 0.7f, 1.2f, ancho * 0.7f, 0.22f, 0.26f, 0.32f); // Sala de máquinas.
            caja(x, techo + 3.2f, z, 0.15f, 4, 0.15f, 0.75f, 0.75f, 0.78f); // Antena.
        } else if (tipo == INDUSTRIAL) { // Galpón: techo plano oscuro y chimenea.
            caja(x, techo + 0.15f, z, ancho + 0.2f, 0.3f, ancho + 0.2f, 0.30f, 0.30f, 0.30f); // Techo.
            caja(x + 2.6f, techo + 2.5f, z + 2.6f, 0.9f, 5, 0.9f, 0.40f, 0.22f, 0.18f); // Chimenea de ladrillo.
        } else { // Residencial y comercial: cornisa más ancha, como el edificio original.
            caja(x, techo + 0.15f, z, ancho + 0.3f, 0.3f, ancho + 0.3f, 0.20f, 0.26f, 0.32f); // Cubierta.
        }

        if (tipo == COMERCIAL) { // Toldo sobre la acera en la fachada sur (hacia +Z).
            caja(x, base + 2.3f, z + ancho / 2 + 0.6f, ancho - 1, 0.12f, 1.2f, 0.90f, 0.90f, 0.86f);
        }
    }

    /** Dibuja las líneas discontinuas de las calles dejando los cruces despejados. */
    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 2 == 0 && columna % 2 == 1) { // Identifica un tramo horizontal situado entre cruces.
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) { // Coloca tres marcas en la celda.
                caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f); // Dibuja una línea alargada en X.
            }
        }
        if (columna % 2 == 0 && fila % 2 == 1) { // Identifica un tramo vertical situado entre cruces.
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) { // Repite las marcas sobre ese tramo.
                caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f); // Dibuja una línea alargada en Z.
            }
        }
    }

    // ==================== 5. DIBUJAR CAJAS Y ENVIAR UNIFORMS ====================

    /** Dibuja una caja sin giro: posición XYZ, tamaño XYZ y color RGB entre 0 y 1. */
    protected void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0); // Reutiliza el dibujo general con un ángulo de cero.
    }

    /** Dibuja el cubo unitario con escala, giro alrededor de Y, posición y color. */
    protected void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        vector("uPos", x, y, z); // Envía la posición del centro de la caja en el mundo.
        vector("uEscala", sx, sy, sz); // Envía el ancho, alto y profundidad de la caja.
        vector("uColor", r, g, b); // Envía las intensidades roja, verde y azul del material.
        decimal("uGiro", angulo); // Envía la orientación en radianes.
        glDrawArrays(GL_TRIANGLES, 0, 36); // Dibuja 12 triángulos: dos por cada una de las seis caras.
    }

    /** Busca un uniform una vez y guarda su ubicación para los siguientes dibujos. */
    protected int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) { // Revisa si esta variable todavía no fue localizada.
            int ubicacion = glGetUniformLocation(programa, nombre); // Pregunta a OpenGL por la dirección del uniform.
            uniforms.put(nombre, ubicacion); // Guarda la respuesta; -1 significa que el shader no usa esa variable.
        }
        return uniforms.get(nombre); // Recupera la ubicación guardada; OpenGL ignora envíos a -1.
    }

    /** Envía tres números reales a una variable vec3 del shader. */
    protected void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z); // Escribe las tres componentes en el programa activo.
    }

    /** Envía un número real a una variable float del shader. */
    protected void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor); // Actualiza el escalar identificado por su nombre.
    }

    /** Envía un número entero a una variable int del shader. */
    protected void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor); // Actualiza un entero, usado también para interruptores 0/1.
    }

    // ==================== 6. SHADERS: CÓDIGO QUE EJECUTA LA GPU ====================

    /** Transforma vértices del cubo al mundo y después a la pantalla. */
    private String vertexShader() {
        return """
            #version 330 core // Selecciona GLSL 3.30 para el contexto OpenGL 3.3.
            layout (location = 0) in vec3 aPos; // Lee la posicion local del vertice desde el VBO.
            layout (location = 1) in vec3 aNormal; // Lee la normal de la cara desde el mismo VBO.
            uniform vec3 uPos; // Recibe el centro de la caja en la ciudad.
            uniform vec3 uEscala; // Recibe el tamano de la caja en cada eje.
            uniform vec3 uOjo; // Recibe la posicion de la camara.
            uniform vec3 uObjetivo; // Recibe el punto que observa la camara.
            uniform float uGiro; // Recibe el giro del objeto alrededor de Y.
            uniform float uAspecto; // Recibe la relacion ancho/alto de la imagen.
            uniform int uMapa; // Selecciona perspectiva (0) o vista superior ortografica (1).
            uniform float uMedioMapa; // Mitad del lado del mundo que cabe en el minimapa.
            out vec3 vMundo; // Envia la posicion mundial al shader de fragmentos.
            out vec3 vNormal; // Envia la normal transformada para la iluminacion de clase3.

            void main() { // OpenGL ejecuta este bloque una vez por vertice.
                float coseno = cos(uGiro); // Calcula el coseno del giro del objeto.
                float seno = sin(uGiro); // Calcula el seno del mismo giro.
                mat3 giro = mat3( // Construye la matriz de rotacion; GLSL recibe sus columnas.
                    coseno, 0.0, -seno, // Primera columna: direccion del eje X rotado.
                    0.0, 1.0, 0.0, // Segunda columna: Y permanece vertical.
                    seno, 0.0, coseno // Tercera columna: direccion del eje Z rotado.
                ); // Completa la matriz de tres filas y tres columnas.
                vMundo = giro * (aPos * uEscala) + uPos; // Escala, gira y traslada el vertice al mundo.
                vNormal = normalize(giro * (aNormal / uEscala)); // Corrige la normal con la inversa transpuesta de escala y giro.

                if (uMapa == 1) { // Esta rama se usa al dibujar el minimapa en clase4.
                    float pantallaX = vMundo.x / uMedioMapa; // Ajusta el ancho del mundo al intervalo visible -1 a 1.
                    float pantallaY = -vMundo.z / uMedioMapa; // Coloca el norte (-Z) en la parte superior del mapa.
                    float profundidad = -vMundo.y / 100.0; // Hace que los objetos mas altos se vean por encima.
                    gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0); // Proyecta sin reducir objetos lejanos.
                } else { // La escena principal utiliza una camara con perspectiva.
                    vec3 frente = normalize(uObjetivo - uOjo); // Calcula la direccion hacia la que mira la camara.
                    vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0))); // Obtiene el eje horizontal de la camara.
                    vec3 arriba = cross(derecha, frente); // Obtiene el eje vertical perpendicular a los otros dos.
                    vec3 diferencia = vMundo - uOjo; // Traslada el origen del mundo hasta la camara.
                    float vistaX = dot(diferencia, derecha); // Mide cuanto esta el vertice a la derecha de la camara.
                    float vistaY = dot(diferencia, arriba); // Mide cuanto esta el vertice encima de la camara.
                    float vistaZ = -dot(diferencia, frente); // Usa Z negativa delante de la camara, como espera OpenGL.
                    float factor = 1.0 / tan(radians(55.0) * 0.5); // Convierte el campo visual de 55 grados en escala de perspectiva.
                    float cerca = 0.1; // Define la distancia minima visible.
                    float lejos = 320.0; // Define la distancia maxima visible; cubre la ciudad ampliada.
                    float clipX = vistaX * factor / uAspecto; // Corrige la coordenada horizontal segun el ancho de la pantalla.
                    float clipY = vistaY * factor; // Aplica el campo visual a la coordenada vertical.
                    float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ; // Calcula el termino de profundidad dependiente de Z.
                    clipZ += 2.0 * lejos * cerca / (cerca - lejos); // Anade el termino constante de profundidad.
                    gl_Position = vec4(clipX, clipY, clipZ, -vistaZ); // OpenGL dividira XYZ por W para producir perspectiva.
                }
            }
            """; // Termina la cadena que contiene el shader de vértices.
    }

    /** Define el color plano inicial; clase3 reemplaza este shader por la iluminación. */
    protected String fragmentShader() {
        return """
            #version 330 core // Indica la version del lenguaje del shader.
            uniform vec3 uColor; // Recibe el color RGB enviado por cajaGirada().
            out vec4 color; // Define el color final que se escribe en la imagen.
            void main() { // Se ejecuta para cada fragmento de la geometria dibujada.
                color = vec4(uColor, 1.0); // Copia el material y establece opacidad completa.
            }
            """; // Termina la cadena del shader de fragmentos.
    }

    /** Compila un shader y comunica el mensaje del controlador si hay un error GLSL. */
    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo); // Reserva un shader del tipo vértice o fragmento.
        glShaderSource(shader, fuente); // Entrega a OpenGL el texto GLSL.
        glCompileShader(shader); // Solicita la compilación para esta GPU.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) { // Consulta si la compilación falló.
            String error = glGetShaderInfoLog(shader); // Recupera el detalle del error antes de borrar el objeto.
            glDeleteShader(shader); // Libera el shader que no pudo compilarse.
            throw new IllegalStateException(error); // Detiene la aplicación con el mensaje de diagnóstico.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /** Une los shaders en un programa ejecutable y libera los objetos intermedios. */
    private void crearPrograma() {
        int shaderVertices = compilar(GL_VERTEX_SHADER, vertexShader()); // Compila las transformaciones geométricas.
        int shaderFragmentos = 0; // Permite saber si el segundo shader llegó a crearse.
        try { // Garantiza que ambos objetos intermedios se liberen incluso ante un fallo.
            shaderFragmentos = compilar(GL_FRAGMENT_SHADER, fragmentShader()); // Compila el color de la etapa actual.
            programa = glCreateProgram(); // Crea el contenedor que enlazará ambos shaders.
            glAttachShader(programa, shaderVertices); // Adjunta el shader que calcula posiciones.
            glAttachShader(programa, shaderFragmentos); // Adjunta el shader que calcula colores.
            glLinkProgram(programa); // Enlaza las entradas y salidas de ambos shaders.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) { // Comprueba que el enlace haya sido válido.
                throw new IllegalStateException(glGetProgramInfoLog(programa)); // Muestra incompatibilidades del programa.
            }
        } finally { // Ya no hacen falta objetos shader separados después del enlace.
            glDeleteShader(shaderVertices); // Libera el objeto del shader de vértices.
            if (shaderFragmentos != 0) { // Comprueba que el segundo objeto exista.
                glDeleteShader(shaderFragmentos); // Libera el objeto del shader de fragmentos.
            }
        }
    }

    // ==================== 7. GEOMETRÍA DEL CUBO: VAO Y VBO ====================

    /** Crea un cubo explícito, con posición XYZ y normal XYZ por vértice, como en los ejemplos. */
    private void crearCubo() {
        float[] vertices = { // Cada línea describe un vértice: X, Y, Z, normalX, normalY, normalZ.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina inferior izquierda, normal hacia +Z.
            0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina inferior derecha.
            0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina superior derecha; termina el primer triángulo.
            0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: repite la esquina para el segundo triángulo.
            -0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina superior izquierda.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: cierra el segundo triángulo.

            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: primer vértice, normal hacia -Z.
            -0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: segundo vértice.
            0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: termina el primer triángulo.
            0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: inicia el segundo triángulo.
            0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: esquina inferior derecha.
            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: cierra la cara.

            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda: primer vértice, normal hacia -X.
            -0.5f, 0.5f, -0.5f, -1, 0, 0, // Cara izquierda: segundo vértice.
            -0.5f, -0.5f, -0.5f, -1, 0, 0, // Cara izquierda: termina el primer triángulo.
            -0.5f, -0.5f, -0.5f, -1, 0, 0, // Cara izquierda: inicia el segundo triángulo.
            -0.5f, -0.5f, 0.5f, -1, 0, 0, // Cara izquierda: esquina inferior frontal.
            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda: cierra la cara.

            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha: primer vértice, normal hacia +X.
            0.5f, -0.5f, 0.5f, 1, 0, 0, // Cara derecha: segundo vértice.
            0.5f, -0.5f, -0.5f, 1, 0, 0, // Cara derecha: termina el primer triángulo.
            0.5f, -0.5f, -0.5f, 1, 0, 0, // Cara derecha: inicia el segundo triángulo.
            0.5f, 0.5f, -0.5f, 1, 0, 0, // Cara derecha: esquina superior trasera.
            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha: cierra la cara.

            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: primer vértice, normal hacia +Y.
            -0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: segundo vértice.
            0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: termina el primer triángulo.
            0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: inicia el segundo triángulo.
            0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: esquina trasera derecha.
            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: cierra la cara.

            -0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior: primer vértice, normal hacia -Y.
            0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior: segundo vértice.
            0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: termina el primer triángulo.
            0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: inicia el segundo triángulo.
            -0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: esquina frontal izquierda.
            -0.5f, -0.5f, -0.5f, 0, -1, 0 // Cara inferior: cierra los 36 vértices del cubo.
        };
        vao = glGenVertexArrays(); // Reserva el objeto que recordará la disposición de los atributos.
        glBindVertexArray(vao); // Activa ese VAO para configurarlo.
        vbo = glGenBuffers(); // Reserva el buffer que almacenará los vértices.
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // Selecciona el VBO como destino de datos de vértices.
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Copia los datos a la GPU; el cubo no cambiará.
        int bytesPorVertice = 6 * Float.BYTES; // Cada vértice contiene seis float de cuatro bytes.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, bytesPorVertice, 0L); // Describe XYZ desde el primer byte del vértice.
        glEnableVertexAttribArray(0); // Habilita la posición que recibe aPos.
        glVertexAttribPointer(1, 3, GL_FLOAT, false, bytesPorVertice, 3L * Float.BYTES); // Describe la normal después de XYZ.
        glEnableVertexAttribArray(1); // Habilita la normal que recibe aNormal.
        glBindBuffer(GL_ARRAY_BUFFER, 0); // Deja de seleccionar el VBO una vez configurado.
        glBindVertexArray(0); // Finaliza la configuración del VAO.
    }

    /** Punto de entrada para ejecutar la primera lección. */
    public static void main(String[] args) {
        clase1 aplicacion = new clase1(); // Crea la aplicación que muestra únicamente la ciudad.
        aplicacion.run(); // Inicia la ventana, el ciclo de dibujo y la limpieza final.
    }
}
