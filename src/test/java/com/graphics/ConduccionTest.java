package com.graphics; // Permite probar los métodos protegidos del juego desde el mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de la ciudad, la conducción y el tráfico sin abrir una ventana OpenGL. */
public class ConduccionTest extends TestCase {

    /** Comprueba los mínimos del enunciado: 11 x 11, 12 edificios y 4 parques. */
    public void testTamanoYDistribucionDeLaCiudad() {
        assertTrue(clase1.MAPA.length >= 11); // Al menos 11 filas.
        int edificios = 0;
        int parques = 0;
        for (int[] fila : clase1.MAPA) {
            assertEquals(clase1.MAPA.length, fila.length); // El mapa es cuadrado.
            for (int tipo : fila) {
                if (tipo == clase1.PARQUE) {
                    parques++;
                } else if (tipo != clase1.CALLE) {
                    edificios++;
                }
            }
        }
        assertTrue(edificios >= 12);
        assertTrue(parques >= 4);
        assertEquals(clase1.MAPA.length * clase1.CELDA / 2, clase1.LIMITE, 0f); // El límite sigue al tamaño del mapa.
    }

    /** Comprueba que las calles del borde, las avenidas y los cruces se pueden recorrer. */
    public void testCallesTransitables() {
        clase2 juego = new clase2();
        for (float z = -60; z <= 60; z += 0.25f) {
            assertTrue(juego.puedeCircular(-60, z)); // Calle del borde oeste.
            assertTrue(juego.puedeCircular(0, z)); // Avenida central norte-sur.
        }
        for (float x = -60; x <= 60; x += 0.25f) {
            assertTrue(juego.puedeCircular(x, -60)); // Calle del borde norte.
            assertTrue(juego.puedeCircular(x, 0)); // Avenida central este-oeste.
        }
        assertTrue(juego.puedeCircular(-20, -20)); // Un cruce interior.
    }

    /** Comprueba edificios, parques, aceras y bordes del mapa ampliado. */
    public void testObstaculosYLimites() {
        clase2 juego = new clase2();
        assertFalse(juego.puedeCircular(-50, -50)); // Edificio comercial.
        assertFalse(juego.puedeCircular(50, 50)); // Galpón industrial.
        assertFalse(juego.puedeCircular(-10, -50)); // Parque.
        assertFalse(juego.puedeCircular(-55.5f, -50)); // El círculo del auto invade la acera.
        assertTrue(juego.puedeCircular(-57, -50)); // Separación suficiente de la acera.
        assertFalse(juego.puedeCircular(64, 0)); // Demasiado cerca del borde este.
        assertFalse(juego.puedeCircular(0, -66)); // Fuera del borde norte.
    }

    /** Todos los destinos de entrega están sobre calles transitables. */
    public void testDestinosAccesibles() {
        clase2 juego = new clase2();
        for (float[] destino : clase4.DESTINOS) {
            assertTrue(juego.puedeCircular(destino[0], destino[1]));
        }
    }

    /** Hay al menos nueve farolas y todas están dentro del mapa. */
    public void testFarolas() {
        assertTrue(clase3.LUCES.length >= 9);
        for (float[] luz : clase3.LUCES) {
            assertTrue(Math.abs(luz[0]) < clase1.LIMITE);
            assertTrue(Math.abs(luz[2]) < clase1.LIMITE);
        }
    }

    /** Comprueba que el reinicio restaura la posición y detiene el vehículo. */
    public void testReinicio() {
        clase4 juego = new clase4();
        juego.autoX = 12;
        juego.autoZ = 4;
        juego.velocidad = 10;
        juego.angulo = 2;
        juego.reiniciar(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(clase2.INICIO_X, juego.autoX, 0f);
        assertEquals(clase2.INICIO_Z, juego.autoZ, 0f);
        assertEquals(-60f, juego.autoX, 0f); // Esquina suroeste.
        assertEquals(60f, juego.autoZ, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0f, juego.angulo, 0f);
    }

    /** El tráfico circula durante 100 segundos sin entrar en manzanas ni salir del mapa. */
    public void testTraficoSeMantieneEnLasCalles() {
        clase5 juego = new clase5();
        clase2 ciudad = new clase2(); // Solo manzanas y bordes, sin el propio tráfico.
        assertTrue(juego.trafico.size() >= 3);
        float[] recorrido = new float[juego.trafico.size()];
        for (int paso = 0; paso < 2000; paso++) {
            float[][] antes = new float[juego.trafico.size()][2];
            for (int i = 0; i < juego.trafico.size(); i++) {
                antes[i][0] = juego.trafico.get(i).x;
                antes[i][1] = juego.trafico.get(i).z;
            }
            juego.actualizarTrafico(0.05f);
            for (int i = 0; i < juego.trafico.size(); i++) {
                clase5.Vehiculo v = juego.trafico.get(i);
                assertTrue("vehiculo " + i + " en manzana: " + v.x + ", " + v.z, ciudad.puedeCircular(v.x, v.z));
                assertTrue(Math.abs(v.x) <= clase1.LIMITE && Math.abs(v.z) <= clase1.LIMITE);
                float dx = v.x - antes[i][0];
                float dz = v.z - antes[i][1];
                recorrido[i] += (float) Math.sqrt(dx * dx + dz * dz);
            }
        }
        for (float distancia : recorrido) {
            assertTrue(distancia > 200); // Cada vehículo recorrió varias cuadras y no quedó trabado.
        }
    }

    /** El reinicio también devuelve el tráfico a su posición inicial. */
    public void testReinicioDelTrafico() {
        clase5 juego = new clase5();
        float inicialX = juego.trafico.get(0).x;
        float inicialZ = juego.trafico.get(0).z;
        for (int paso = 0; paso < 200; paso++) {
            juego.actualizarTrafico(0.05f);
        }
        juego.reiniciar();
        assertEquals(inicialX, juego.trafico.get(0).x, 0.001f);
        assertEquals(inicialZ, juego.trafico.get(0).z, 0.001f);
    }
}
