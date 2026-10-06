package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Conserva ciudad y conducción; añade sol, farolas y focos del vehículo.
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 */
public class clase3 extends clase2 {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Inicia la escena con iluminación nocturna.
    protected boolean faros = true; // Inicia los focos del auto encendidos.
    /*
     * Cada fila contiene la posición X, Y, Z de una bombilla. Hay cinco farolas por distrito
     * (cuatro esquinas y el centro), colocadas en la esquina noroeste de la acera de su manzana:
     * centro de la manzana - 4 en X y en Z. Así quedan junto a la calle y lejos del semáforo (+4, -4).
     */
    protected static final float[][] LUCES = {
        // Distrito comercial (noroeste).
        {-54, 4.5f, -54}, {-14, 4.5f, -54}, {-34, 4.5f, -34}, {-54, 4.5f, -14}, {-14, 4.5f, -14},
        // Distrito financiero (noreste).
        {6, 4.5f, -54}, {46, 4.5f, -54}, {26, 4.5f, -34}, {6, 4.5f, -14}, {46, 4.5f, -14},
        // Distrito residencial (suroeste).
        {-54, 4.5f, 6}, {-14, 4.5f, 6}, {-34, 4.5f, 26}, {-54, 4.5f, 46}, {-14, 4.5f, 46},
        // Distrito industrial (sureste).
        {6, 4.5f, 6}, {46, 4.5f, 6}, {26, 4.5f, 26}, {6, 4.5f, 46}, {46, 4.5f, 46}
    };

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de clase2.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Alterna entre noche y día.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que clase2 incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | N: " + ambiente + " | F: faros " + estadoFaros; // Devuelve ambos indicadores y sus teclas.
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    @Override // Sustituye el método vacío de clase1.
    protected void prepararLuces() {
        entero("uNoche", 0); // Inicialmente configura iluminación diurna.
        if (noche) { // Revisa el modo elegido por el usuario.
            entero("uNoche", 1); // Comunica al shader que debe usar iluminación nocturna.
        }
        entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        for (int indice = 0; indice < LUCES.length; indice++) { // Recorre las nueve bombillas.
            float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
            float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
            float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
            vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(angulo); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula hacia dónde apunta el auto en Z.
        vector("uAuto", autoX, 1.0f, autoZ); // Envía el centro del vehículo a la altura de sus focos.
        vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    /** Dibuja postes y bombillas en las mismas posiciones usadas para calcular la luz. */
    @Override // Añade geometría a la ciudad y al auto heredados.
    protected void escena() {
        super.escena(); // Dibuja la ciudad y el vehículo de las primeras dos lecciones.
        for (float[] luz : LUCES) { // Selecciona una farola a la vez.
            float x = luz[0]; // Lee la posición horizontal del poste.
            float y = luz[1]; // Lee la altura de la bombilla.
            float z = luz[2]; // Lee la posición del poste en profundidad.
            caja(x, 2.2f, z, 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f); // Dibuja el poste delgado y oscuro.
            if (noche) { // Las bombillas aparentan estar encendidas únicamente de noche.
                entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            }
            caja(x, y, z, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f); // Dibuja la bombilla de color cálido.
            entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
        }
    }

    // ==================== 5. CÁLCULO DE LUZ EN LA GPU ====================

    /** Devuelve el programa GLSL que calcula el color iluminado de cada fragmento. */
    @Override // Reemplaza el shader de color plano de clase1.
    protected String fragmentShader() {
        return """
            #version 330 core // Selecciona la version GLSL correspondiente a OpenGL 3.3.
            in vec3 vMundo; // Recibe la posicion del fragmento en la ciudad.
            in vec3 vNormal; // Recibe la direccion perpendicular a la superficie.
            in vec3 vLocal; // Recibe la posicion dentro de la pieza (para detalles que viajan con el objeto).
            uniform vec3 uColor; // Recibe el color base de la caja.
            uniform vec3 uLuces[%d]; // Recibe las posiciones de las farolas; Java escribe el tamano real.
            uniform vec3 uAuto; // Recibe la posicion del auto a la altura de los faros.
            uniform vec3 uFrente; // Recibe la direccion hacia la que apunta el vehiculo.
            uniform int uNoche; // Vale 1 de noche y 0 de dia.
            uniform int uFaros; // Vale 1 cuando los focos estan encendidos.
            uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
            uniform int uMapa; // Vale 1 durante el dibujo del minimapa de clase4.
            uniform int uMaterial; // Patron procedural: 0 plano, 1 acera, 2 pared con ventanas, 3 torre, 4 galpon, 5 paso peatonal.
            uniform vec3 uOjo; // Posicion de la camara; el patron fino se desvanece con la distancia.
            uniform vec3 uEscala; // Tamano de la pieza actual; sirve para alinear las ventanas con las esquinas.
            uniform vec3 uLuzVentana; // Color de las ventanas encendidas del edificio actual.
            out vec4 color; // Entrega el color RGBA final al framebuffer.

            float azar(vec2 p) { // Numero pseudoaleatorio entre 0 y 1 que depende solo de la celda p.
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float ruido(vec2 p) { // Ruido suave: interpola el azar de las cuatro esquinas de la celda.
                vec2 celda = floor(p);
                vec2 f = fract(p);
                vec2 suave = f * f * (3.0 - 2.0 * f); // Curva que evita cambios bruscos en los bordes.
                float abajo = mix(azar(celda), azar(celda + vec2(1.0, 0.0)), suave.x);
                float arriba = mix(azar(celda + vec2(0.0, 1.0)), azar(celda + vec2(1.0, 1.0)), suave.x);
                return mix(abajo, arriba, suave.y);
            }

            vec3 emiColor = vec3(0.0); // Luz propia de las ventanas encendidas; se suma al final, sin oscurecerse.

            /*
             * Cuadricula de ventanas alineada con las esquinas del edificio.
             * muro: color de la pared; paso: ancho y alto de cada celda; margen: parte de la celda sin vidrio;
             * soloFila: si es mayor o igual que 0, solo esa fila de celdas lleva ventanas (galpones).
             */
            vec3 ventanas(vec3 muro, vec3 normal, vec2 paso, vec2 margen, float soloFila) {
                bool lateral = abs(normal.x) > 0.5; // Pared perpendicular a X: se recorre en Z.
                float ancho = lateral ? uEscala.z : uEscala.x; // Ancho de esta pared.
                float horizontal = lateral ? vLocal.z : vLocal.x; // Posicion a lo largo de la pared, desde su centro.
                float columnas = max(1.0, floor(ancho / paso.x)); // Cuantas ventanas caben, para que el borde quede simetrico.
                float filas = max(1.0, floor(uEscala.y / paso.y));
                vec2 celda = vec2((horizontal + ancho * 0.5) * columnas / ancho, (vLocal.y + uEscala.y * 0.5) * filas / uEscala.y);
                vec2 id = floor(celda); // Numero de columna y de piso de esta ventana.
                vec2 borde = min(fract(celda), 1.0 - fract(celda)); // Distancia al borde de la celda.
                float dentro = smoothstep(margen.x, margen.x + 0.05, borde.x) * smoothstep(margen.y, margen.y + 0.05, borde.y);
                if (soloFila >= 0.0 && id.y != soloFila) {
                    dentro = 0.0;
                }

                vec2 bloque = floor((vMundo.xz + 65.0) / 10.0); // Identifica la manzana para que cada edificio sea distinto.
                float cara = normal.x + normal.z * 2.0; // Identifica la pared (-2, -1, 1 o 2).
                float sorteo = azar(id + bloque * vec2(7.0, 13.0) + vec2(cara * 5.0, cara * 3.0));
                float tono = azar(id.yx + bloque * vec2(11.0, 5.0) + cara);
                bool encendida = uNoche == 1 && sorteo > 0.70; // Cerca del 30 por ciento encendidas de noche.

                vec3 reflejoCielo = vec3(0.12, 0.17, 0.24) * smoothstep(2.0, 30.0, vMundo.y); // Los pisos altos reflejan mas cielo.
                vec3 vidrio = vec3(0.11, 0.15, 0.21) * (0.75 + 0.6 * tono) + reflejoCielo;
                vec3 luzVentana = mix(uLuzVentana, vec3(0.85, 0.92, 1.0), step(0.75, tono) * 0.5); // Algunas ventanas con luz mas fria.
                luzVentana *= 0.62 + 0.26 * tono;

                float cercaFino = 1.0 - smoothstep(0.2, 0.5, max(fwidth(celda.x), fwidth(celda.y))); // 0 cuando las ventanas son casi un pixel.
                float cobertura = (1.0 - 2.0 * margen.x) * (1.0 - 2.0 * margen.y);
                if (soloFila >= 0.0) {
                    cobertura /= filas;
                }
                vec3 detalle = mix(muro, vidrio, dentro);
                vec3 promedio = mix(muro, vec3(0.15, 0.20, 0.28), cobertura);
                float luzDetalle = encendida ? dentro : 0.0;
                emiColor = mix(uLuzVentana * cobertura * 0.30 * float(uNoche), luzVentana * luzDetalle, cercaFino);
                return mix(promedio, detalle, cercaFino);
            }

            vec3 textura(vec3 base, vec3 normal) { // Devuelve el color base modulado por el patron del material.
                float d = length(vMundo - uOjo); // Distancia del fragmento a la camara.
                float lejos = smoothstep(35.0, 100.0, d); // 0 cerca, 1 lejos: evita parpadeo del detalle fino.

                if (uMaterial == 1) { // Acera: baldosas de 1.25 con juntas finas y variacion de tono.
                    if (normal.y < 0.5) {
                        return base;
                    }
                    vec2 p = vMundo.xz / 1.25;
                    vec2 f = fract(p);
                    float distanciaJunta = min(min(f.x, 1.0 - f.x), min(f.y, 1.0 - f.y));
                    float junta = (1.0 - smoothstep(0.0, 0.04, distanciaJunta)) * (1.0 - lejos);
                    float variacion = azar(floor(p)) - 0.5;
                    float mancha = ruido(vMundo.xz * 0.8) - 0.5;
                    return base * (1.0 + variacion * 0.12 + mancha * 0.08) * (1.0 - 0.3 * junta);
                }

                if (uMaterial == 5) { // Paso peatonal: franjas de 0.5 cada 1.0 repartidas a lo ancho de la calle.
                    float largo = uEscala.x > uEscala.z ? vLocal.x : vLocal.z; // Las franjas se reparten sobre el lado mayor.
                    float franja = step(0.5, fract(largo + 0.75)); // Una franja centrada en cada valor entero.
                    float cercaFino = 1.0 - smoothstep(0.3, 0.6, fwidth(largo)); // De lejos se funde en un gris medio.
                    franja = mix(0.5, franja, cercaFino);
                    return mix(vec3(0.16, 0.19, 0.23), base, franja); // Entre franjas se ve el color del asfalto.
                }

                if (uMaterial < 2 || abs(normal.y) > 0.5) { // Solo las paredes laterales llevan patron de fachada.
                    return base;
                }
                float u = abs(normal.x) > 0.5 ? vMundo.z : vMundo.x; // Coordenada horizontal a lo largo de la pared.
                float y = vMundo.y; // Altura sobre el suelo.

                if (uMaterial == 2) { // Pared lisa con ventanas: solo una variacion de tono muy suave, como revoque.
                    float revoque = ruido(vec2(u, y) * 0.6) - 0.5;
                    return ventanas(base * (1.0 + revoque * 0.08), normal, vec2(1.3, 1.8), vec2(0.20, 0.20), -1.0);
                }

                if (uMaterial == 3) { // Torre de oficinas: muro cortina con ventanas muy juntas.
                    float reflejo = 0.9 + 0.2 * ruido(vec2(u * 0.2, y * 0.1));
                    return ventanas(base * reflejo, normal, vec2(0.9, 1.3), vec2(0.10, 0.12), -1.0);
                }

                float onda = 0.5 + 0.5 * sin(u * 12.566); // Galpon: chapa acanalada con una franja de ventanas altas.
                vec3 chapa = mix(base * (0.82 + 0.28 * onda), base * 0.96, lejos);
                return ventanas(chapa, normal, vec2(1.8, 1.4), vec2(0.15, 0.20), 1.0);
            }

            void main() { // Se ejecuta para cada fragmento visible de una caja.
                if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
                    color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
                    return; // Termina el shader sin calcular iluminacion.
                }

                vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
                vec3 base = textura(uColor, normal); // Color del material despues de aplicar el patron.
                vec3 luz = vec3(0.48); // Define la luz ambiental diurna que llega a todas las caras.
                float intensidadSol = 0.65; // Define la fuerza de la iluminacion direccional diurna.
                if (uNoche == 1) { // Ajusta el ambiente si es de noche.
                    luz = vec3(0.15, 0.19, 0.28); // Usa una luz ambiental tenue y azulada que aun permite conducir.
                    intensidadSol = 0.10; // Conserva una pequena luz direccional nocturna.
                }
                vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // Define una fuente lejana por su direccion.
                float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe mas luz si mira al sol.
                luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribucion direccional al ambiente.

                if (uNoche == 1) { // Calcula la iluminacion de las farolas solo de noche.
                    for (int indice = 0; indice < %d; indice++) { // Acumula el aporte de cada bombilla.
                        vec3 haciaLuz = uLuces[indice] - vMundo; // Forma el vector desde la superficie hacia la farola.
                        float distancia = length(haciaLuz); // Mide cuantas unidades separan superficie y bombilla.
                        float difusa = max(dot(normal, normalize(haciaLuz)), 0.0); // Calcula la incidencia de la luz sobre la cara.
                        float atenuacion = 1.0 + 0.12 * distancia + 0.03 * distancia * distancia; // Reduce el alcance con la distancia.
                        vec3 colorFarola = vec3(1.0, 0.73, 0.34); // Define el tono calido de la farola.
                        luz += colorFarola * difusa * 3.0 / atenuacion; // Suma el aporte atenuado de esta bombilla.
                    }
                }

                if (uFaros == 1) { // Calcula los conos unicamente si estan encendidos.
                    for (int indice = 0; indice < 2; indice++) { // Repite el calculo para los dos faros.
                        vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Obtiene la direccion hacia el lado derecho del auto.
                        float separacion = -0.64; // Selecciona inicialmente el faro izquierdo.
                        if (indice == 1) { // Comprueba si corresponde calcular el segundo faro.
                            separacion = 0.64; // Desplaza el segundo faro al lado derecho.
                        }
                        vec3 origen = uAuto + uFrente * 1.6 + lateral * separacion; // Ubica el faro delante de la carroceria.
                        vec3 haciaSuperficie = vMundo - origen; // Forma el vector del faro al fragmento.
                        float distancia = length(haciaSuperficie); // Mide la distancia recorrida por la luz.
                        vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Inclina el foco ligeramente hacia el suelo.
                        float alineacion = dot(normalize(haciaSuperficie), eje); // Un valor cercano a 1 indica el centro del haz.
                        float cono = smoothstep(0.85, 0.97, alineacion); // Suaviza el borde entre el exterior y el interior del foco.
                        float difusa = max(dot(normal, -normalize(haciaSuperficie)), 0.0); // Mide cuanto mira la cara hacia el faro.
                        float atenuacion = 1.0 + 0.04 * distancia * distancia; // Disminuye la intensidad al alejarse.
                        vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Define una luz frontal blanca y calida.
                        luz += colorFaro * cono * difusa * 8.0 / atenuacion; // Anade el aporte del foco a la iluminacion total.
                    }
                }

                color = vec4(base * luz + emiColor, 1.0); // Material iluminado mas la luz propia de las ventanas.
            }
            """.formatted(LUCES.length, LUCES.length); // Inserta la cantidad de farolas en los dos %d del texto GLSL.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        clase3 aplicacion = new clase3(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
