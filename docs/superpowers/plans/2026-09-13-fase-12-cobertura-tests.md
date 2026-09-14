# Fase 12 — Instrumentar cobertura de tests

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Goal:** El `ROADMAP.md` pide "aumentar la cobertura de pruebas unitarias y de integración al 85%", pero hoy no hay ninguna herramienta que mida cobertura en el proyecto — no se puede saber el número actual ni la brecha real. Esta fase agrega JaCoCo, lo conecta a CI (que es la única fuente de verdad confiable para la suite completa, ver más abajo), y entrega un reporte de cobertura real + un análisis de qué paquetes están más desnudos. No incluye escribir tests nuevos para cerrar la brecha — eso se dimensiona en una fase siguiente una vez conocido el número real.

**Architecture:** Sin cambios de arquitectura de producción. Se agrega el plugin `jacoco` (built-in de Gradle) a `build.gradle`, conectado como `finalizedBy` de la task `test` para que el reporte se genere aunque algún test falle. El workflow de CI (`.github/workflows/ci.yml`) ya corre `./gradlew test` con Docker nativo de Ubuntu — solo hace falta subir el reporte de JaCoCo como artifact, igual que ya hace con el reporte de tests.

**Tech Stack:** Gradle 8.13 (plugin `jacoco` built-in, toolVersion por defecto de esta versión de Gradle es 0.8.12), GitHub Actions (`actions/upload-artifact@v4`, ya usado en el workflow).

**Spec:** Este plan documenta su propio spec — verificado en vivo durante el brainstorming de esta fase, no es una suposición:

- **No hay JaCoCo ni ninguna herramienta de cobertura hoy**: `grep -i jacoco build.gradle` → cero resultados. Confirmado leyendo `build.gradle` completo (líneas 1-101): solo plugins `java`, `org.springframework.boot`, `io.spring.dependency-management`.
- **CI ya corre la suite completa con éxito**: `.github/workflows/ci.yml` corre en `ubuntu-latest`, sin `services:` explícitos porque "Testcontainers levanta PostgreSQL, Redis y Elasticsearch bajo demanda usando el Docker del runner" (comentario ya existente en el propio workflow, línea 13-14). No hay evidencia de que esos runners tengan el problema descrito abajo — es específico de este entorno de desarrollo Windows.
- **Los "10 tests conocidos que fallan" localmente en este entorno NO son un límite real de "falta Docker"**: se investigó en profundidad durante el brainstorming de esta fase.
  - `docker version`/`docker info` funcionan perfectamente por CLI: Docker Desktop 4.82.0, Engine 29.6.1, API 1.55 (mínimo soportado 1.40), contexto activo `desktop-linux` con endpoint `npipe:////./pipe/dockerDesktopLinuxEngine`.
  - Los 10 tests (`EcommerceApplicationTests`, `OutboxIndexacionIntegrationTest`, `ServicioCarritoIntegrationTest`, `ListadoCatalogoNMasUnoIntegrationTest`, `ReservaInventarioConcurrenciaIntegrationTest`, `AislamientoTenantGraphQLIntegrationTest`, `AislamientoMultiTenantIntegrationTest`, `SpoofingHeaderInquilinoIntegrationTest`, `CasoUsoOrdenIntegrationTest`, `CuponConcurrenciaIntegrationTest`) fallan todos con `initializationError` → `java.lang.IllegalStateException: Could not find a valid Docker environment`.
  - Se probó `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` (el pipe correcto según `docker context ls`) y también fijar `DOCKER_API_VERSION=1.41` (forzar una versión de API más vieja) — **ninguno de los dos cambió el resultado**. Tanto `NpipeSocketClientProviderStrategy` como `EnvironmentAndSystemPropertyClientProviderStrategy` reciben del daemon un `BadRequestException (Status 400)` con un body de `docker info` completamente vacío (todos los campos en su valor por defecto: `""`, `0`, `false`, etc.), no un error de conexión — el daemon responde, pero con algo que el cliente `docker-java` (que trae Testcontainers 1.20.6, de 2024) no puede interpretar como una respuesta válida de la versión de API 1.55 que expone Docker Desktop 4.82.0.
  - Conclusión: es una incompatibilidad de versión entre `docker-java`/Testcontainers 1.20.6 y Docker Desktop 4.82.0 en este entorno, no una falta de Docker ni un pipe mal configurado. Arreglarla de raíz implicaría subir la versión de Testcontainers (no probado, no en el alcance de esta fase — ver Global Constraints) o downgradear Docker Desktop (decisión del usuario sobre su máquina, no de este repo). **Decisión tomada con el usuario: no perseguir el fix local en esta fase — usar CI como fuente de verdad.**

## Global Constraints

- **No subir la versión de `testcontainersVersion` en `build.gradle`** (hoy `1.20.6`) como parte de esta fase — es un cambio de dependencia más amplio, no probado, y el usuario ya decidió no perseguirlo acá (ver más arriba). Si en el futuro se quiere intentar, es una fase propia.
- **No agregar ningún gate de cobertura mínima todavía** (nada de `jacocoTestCoverageVerification` con un `minimum` que rompa el build) — el número real todavía no se conoce, así que no se puede fijar un umbral con criterio. Esta fase es solo instrumentación + reporte.
- El reporte de JaCoCo debe generarse aunque `test` falle (los 10 conocidos, solo en este entorno local) — usar `finalizedBy`, no `dependsOn` a secas, para que el reporte no se salte cuando el build de CI (que si pasa 481/481) también lo necesite sin sorpresas de orden de tasks.
- Commits en español, Conventional Commits, uno por tarea — sin `Co-Authored-By` de ningún asistente de IA.
- El reporte de cobertura real (Task 4) sale de una corrida de **CI**, no de este entorno local — no reportar el número de cobertura de una corrida local parcial (sin los 10 tests) como si fuera el número real, dejarlo explícito en el reporte de qué corrida sale.

---

### Task 1: Agregar el plugin JaCoCo a `build.gradle`

**Files:**
- Modify: `build.gradle`

**Interfaces:** Ninguna — cambio de configuración de build, no de código de producción.

- [ ] **Step 1: Agregar el plugin `jacoco` al bloque `plugins`**

En `build.gradle`, líneas 1-5, agregar `id 'jacoco'`:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.4'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'jacoco'
}
```

- [ ] **Step 2: Fijar la versión de JaCoCo y conectar el reporte a la task `test`**

Al final de `build.gradle` (después del bloque `tasks.named('test') { useJUnitPlatform() }` existente, líneas 99-101), agregar:

```groovy
jacoco {
    toolVersion = "0.8.12"
}

tasks.named('test') {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

tasks.named('jacocoTestReport') {
    dependsOn tasks.named('test')
    reports {
        xml.required = true
        html.required = true
    }
}
```

Nota: esto reemplaza el bloque `tasks.named('test') { useJUnitPlatform() }` existente (líneas 99-101) — no dejar dos bloques `tasks.named('test')` separados, fusionarlos en uno solo como está arriba.

- [ ] **Step 3: Verificar que el plugin genera el reporte, aunque los 10 tests conocidos fallen**

Correr:
```bash
./gradlew test jacocoTestReport --continue
```

Resultado esperado: el build termina en `BUILD FAILED` (por los 10 `initializationError` ya conocidos y documentados — esperado en este entorno, no es una regresión), pero la task `jacocoTestReport` **sí se ejecuta** (buscar `> Task :jacocoTestReport` en el output, sin `SKIPPED`). Confirmar que existe:

```bash
ls build/reports/jacoco/test/html/index.html
ls build/reports/jacoco/test/jacocoTestReport.xml
```

Ambos archivos deben existir. Abrir `index.html` (o leer el resumen desde la terminal) y confirmar que muestra un porcentaje de cobertura por instrucciones distinto de 0% — es un número parcial (sin los 10 tests que no corren acá), no el número real de la fase; el número real sale en la Task 4 desde CI.

- [ ] **Step 4: Commit**

```bash
git add build.gradle
git commit -m "chore(build): agregar plugin JaCoCo para medir cobertura de tests"
```

---

### Task 2: Subir el reporte de JaCoCo como artifact de CI

**Files:**
- Modify: `.github/workflows/ci.yml`

**Interfaces:** Ninguna — cambio de CI, no de código de producción. Depende de Task 1 (la task `jacocoTestReport` debe existir para que este step tenga algo que subir).

- [ ] **Step 1: Agregar un step que suba el reporte HTML y XML de JaCoCo**

En `.github/workflows/ci.yml`, después del step `Publish Test Report` existente (líneas 37-42) y antes de `Build Docker Image` (línea 44), agregar:

```yaml
    - name: Publish Coverage Report (JaCoCo)
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: jacoco-coverage-report
        path: build/reports/jacoco/test/
```

No hace falta un step de Gradle nuevo: la task `test` ya quedó `finalizedBy jacocoTestReport` en la Task 1, así que el step existente `Run Integration Tests with Testcontainers` (que corre `./gradlew test`) ya genera el reporte de JaCoCo como efecto secundario.

- [ ] **Step 2: Revisar el YAML resultante**

Correr:
```bash
cat .github/workflows/ci.yml
```

Confirmar indentación consistente con los steps existentes (2 espacios por nivel, mismo estilo que `Publish Test Report`) y que el nuevo step quedó entre `Publish Test Report` y `Build Docker Image`, no verificable localmente de otra forma (no hay runner de GitHub Actions local) — se confirma de verdad en la Task 4, cuando CI corra sobre el PR de esta fase.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: publicar el reporte de cobertura JaCoCo como artifact"
```

---

### Task 3: Documentar la causa raíz real del gap de Testcontainers en este entorno

**Files:**
- Modify: `CLAUDE.md` (raíz del proyecto, sección `## Testing`, líneas 53-57)

**Interfaces:** Ninguna — solo documentación.

- [ ] **Step 1: Agregar una nota a la sección `## Testing` de `CLAUDE.md`**

Después de la última línea de la sección (línea 57: `- Rest Assured (`spring-mock-mvc`) disponible para tests de controllers.`), agregar:

```markdown
- **Testcontainers puede fallar en entornos locales con Docker Desktop muy nuevo** (verificado con Docker Desktop 4.82.0 / Engine API 1.55): los `*IntegrationTest` (y `EcommerceApplicationTests`, que también carga el contexto completo) fallan con `IllegalStateException: Could not find a valid Docker environment` aunque `docker version`/`docker info` funcionen perfectamente por CLI. Causa confirmada: el cliente `docker-java` que trae Testcontainers 1.20.6 no negocia bien la respuesta de esa versión de API — tanto `NpipeSocketClientProviderStrategy` como forzar `DOCKER_HOST`/`DOCKER_API_VERSION` a mano dan el mismo `BadRequestException (Status 400)` con un body de `docker info` vacío. No es un pipe mal configurado ni falta de Docker. No hay fix local confirmado todavía (subir `testcontainersVersion` en `build.gradle` es la vía más probable, no probado — ver [`docs/superpowers/plans/2026-09-13-fase-12-cobertura-tests.md`](docs/superpowers/plans/2026-09-13-fase-12-cobertura-tests.md)). **CI es la fuente de verdad para la suite completa** — corre en `ubuntu-latest` con Docker nativo, sin este problema.
```

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: documentar la causa raiz real del gap de Testcontainers en Windows"
```

---

### Task 4: Abrir el PR, obtener el reporte real de CI y documentar el baseline de cobertura

**Files:**
- Create: `docs/superpowers/plans/2026-09-13-fase-12-cobertura-baseline.md`

**Interfaces:** Consume el artifact `jacoco-coverage-report` producido por el workflow de CI de la Task 2, sobre el PR abierto con los commits de las Tasks 1-3.

- [ ] **Step 1: Pushear la rama y abrir el PR**

(Asumir que ya se hicieron los commits de las Tasks 1-3 en una rama `chore/cobertura-tests-jacoco` creada desde `master`.)

```bash
git push -u origin chore/cobertura-tests-jacoco
gh pr create --title "chore(build): instrumentar cobertura de tests con JaCoCo — Fase 12" --body "Ver docs/superpowers/plans/2026-09-13-fase-12-cobertura-tests.md"
```

- [ ] **Step 2: Esperar a que termine el workflow de CI sobre el PR**

```bash
gh pr checks <numero-de-pr> --watch
```

Esperado: el job `build-and-test` termina en verde (481/481 — a diferencia de este entorno local, CI no tiene el problema de Testcontainers documentado en la Task 3).

- [ ] **Step 3: Descargar el artifact de JaCoCo generado por CI**

```bash
gh run list --branch chore/cobertura-tests-jacoco --limit 1 --json databaseId --jq '.[0].databaseId'
```

Con el `databaseId` obtenido (ej. `12345`):

```bash
gh run download 12345 --name jacoco-coverage-report --dir /tmp/jacoco-ci
```

(En Windows, usar una carpeta bajo el directorio de scratchpad de la sesión en vez de `/tmp` si `/tmp` no es accesible.)

- [ ] **Step 4: Extraer el número real de cobertura y las clases/paquetes más desnudos**

Leer el resumen desde el HTML descargado:

```bash
grep -A2 "Total" /tmp/jacoco-ci/html/index.html | head -20
```

Esto da el porcentaje total de cobertura de instrucciones y de ramas (branches) de la suite completa (481/481 verdes en CI). Adicionalmente, abrir `/tmp/jacoco-ci/html/index.html` y navegar a los 3-5 paquetes con menor porcentaje de cobertura (la tabla en la página principal está ordenada por nombre, no por porcentaje — hay que revisar cada fila) para identificar los candidatos más obvios a cubrir en una fase futura.

- [ ] **Step 5: Escribir el reporte de baseline**

Crear `docs/superpowers/plans/2026-09-13-fase-12-cobertura-baseline.md` con este formato (reemplazar los valores entre `<>` con los números reales obtenidos en el Step 4 — no dejar placeholders sin reemplazar):

```markdown
# Fase 12 — Baseline de cobertura de tests

Medido con JaCoCo sobre la corrida de CI del PR de esta fase (run `<databaseId>`, commit `<sha>`), 481/481 tests verdes (a diferencia de este entorno local, donde 10 no corren — ver `CLAUDE.md` sección Testing).

## Número global

- Cobertura de instrucciones: `<X>`%
- Cobertura de ramas (branches): `<X>`%

## Paquetes con menor cobertura (candidatos para una fase futura)

1. `<paquete>` — `<X>`% instrucciones
2. `<paquete>` — `<X>`% instrucciones
3. `<paquete>` — `<X>`% instrucciones

## Brecha hacia el 85% del ROADMAP

`<análisis breve: cuántos puntos porcentuales faltan, y si los paquetes de arriba explican la mayor parte de la brecha o está repartida>`.

## Siguiente paso sugerido

Fase 13 (a definir): escribir tests dirigidos a los paquetes de arriba. No incluido en el alcance de la Fase 12.
```

- [ ] **Step 6: Commit y push**

```bash
git add docs/superpowers/plans/2026-09-13-fase-12-cobertura-baseline.md
git commit -m "docs: agregar el baseline real de cobertura medido en CI — Fase 12"
git push
```
