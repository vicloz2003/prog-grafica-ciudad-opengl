# Ciudad interactiva con OpenGL

Ampliación del proyecto base del curso (Java 17, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO). Toda la geometría se genera con código a partir de un único cubo; no hay imágenes ni modelos externos.

## Ejecutar

Abrir una terminal en esta carpeta (donde está `pom.xml`). Se necesita JDK 17 o superior y Maven.

```sh
mvn compile exec:exec
```

Ese comando abre la versión final (`clase5`). Para ver una etapa concreta:

```sh
mvn compile exec:exec -DmainClass=com.graphics.clase1
mvn compile exec:exec -DmainClass=com.graphics.clase2
mvn compile exec:exec -DmainClass=com.graphics.clase3
mvn compile exec:exec -DmainClass=com.graphics.clase4
mvn compile exec:exec -DmainClass=com.graphics.clase5
```

Pruebas de lógica (no abren ventana):

```sh
mvn test
```

## Organización

Cada etapa hereda de la anterior y agrega una sola responsabilidad:

```text
clase1  ciudad 13x13, distritos, renderizador y shaders
  └── clase2  conducción, colisiones y cámara de seguimiento
        └── clase3  iluminación: sol, farolas y faros
              └── clase4  ventanas, parques, semáforos, entregas y minimapa
                    └── clase5  tráfico autónomo
```

## Diseño de la ciudad

- Mapa de **13 × 13 celdas** de 10 unidades (130 × 130). Filas y columnas pares son calles; las impares son manzanas (36 en total: 30 edificios y 6 parques).
- Las avenidas centrales (fila 6 y columna 6) dividen la ciudad en cuatro distritos:

| Distrito | Edificios | Luz de ventanas de noche |
|---|---|---|
| Comercial (noroeste) | Medianos, colores vivos, toldo | Neón magenta / cian |
| Financiero (noreste) | Rascacielos de 18 a 30 con antena | Blanco frío |
| Residencial (suroeste) | Bajos, terracota o crema | Amarillo cálido |
| Industrial (sureste) | Galpones con chimenea | Naranja |

- Suelo, límites de circulación, colisiones, cámara orbital y escala del minimapa se calculan a partir de `LIMITE = MAPA.length * CELDA / 2`, así que cambiar el tamaño del mapa no requiere tocar números sueltos.
- 20 farolas (5 por distrito) iluminan el suelo y los edificios cercanos con atenuación por distancia. El shader recibe la cantidad real de farolas.
- El jugador conduce una camioneta todoterreno negra. Hay 4 entregas, una por distrito; el destino activo se marca con una baliza dorada y en el minimapa.
- Tráfico: taxi, autobús, furgoneta de reparto y patrulla con luces intermitentes. Van de cruce en cruce por el carril derecho, eligen al azar hacia dónde seguir, frenan si hay algo delante y el jugador no puede atravesarlos.

## Controles

| Tecla | Acción |
|---|---|
| W / S o ↑ / ↓ | Acelerar / frenar y retroceder |
| A / D o ← / → | Girar mientras el auto se mueve |
| Espacio | Freno |
| C | Cámara de seguimiento / vista aérea |
| N | Día / noche |
| F | Faros |
| M | Mostrar / ocultar minimapa |
| R | Reiniciar auto, entregas y tráfico |
| ESC | Salir |

En `clase1` las flechas izquierda/derecha orbitan la cámara. El título de la ventana muestra velocidad, estado de luces, entregas y tiempo.

## Limitaciones

- La iluminación es local: no hay sombras y una farola puede iluminar a través de un edificio.
- Los semáforos son decorativos; no detienen al auto ni al tráfico.
- El tráfico no respeta prioridades en los cruces: solo frena ante el jugador o ante un vehículo que va en su mismo sentido.
- Chocar detiene al auto; no hay rebote ni deslizamiento contra las paredes.

## Recursos externos

Ninguno. El código parte del proyecto base entregado en clase.
