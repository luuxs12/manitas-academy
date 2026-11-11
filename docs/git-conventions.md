# Convenciones de Git — Mundo Manitas

Reglas del repositorio. Aplican a todos los que commitean, incluido el agente.

## 1. Ramas

```
main                    → siempre estable y desplegable
feature/<tema>          → una funcionalidad
fix/<tema>              → una corrección
docs/<tema>             → documentación
```

- **Nadie commitea directo a `main`.** Se entra por merge (o Pull Request).
- Cada integrante trabaja en su propia rama. La rama personal lleva su nombre:
  `feature/luuxs-auth`, `feature/luuxs-cursos`.
- Una rama = un tema. Si aparece otra cosa, se abre otra rama.
- La rama se borra después del merge.

Ejemplo:

```
main
 └── feature/luuxs-auth
      ├── feat(auth): agregar entidad User y su repositorio
      ├── feat(auth): registrar usuarios con BCrypt
      └── feat(auth): proteger rutas privadas por rol
```

## 2. Formato del commit

```
<tipo>(<alcance>): <descripción>

<por qué se hizo este cambio>

<detalle opcional de qué se tocó>
```

Reglas del asunto (primera línea):

- Máximo **72 caracteres**.
- Minúscula inicial, sin punto final.
- Verbo en imperativo: *agregar*, *corregir*, *mover*; no *agregué* ni *agregando*.
- Tipos y alcances en inglés; la descripción en español.
- Un commit = **un solo cambio lógico**.

### Tipos

| Tipo | Cuándo |
|---|---|
| `feat` | Funcionalidad nueva para el usuario |
| `fix` | Corrección de un error |
| `refactor` | Cambio interno sin cambiar el comportamiento |
| `test` | Pruebas agregadas o corregidas |
| `docs` | Documentación |
| `style` | Formato, espacios, CSS sin cambio de lógica |
| `perf` | Mejora de rendimiento |
| `build` | `pom.xml`, dependencias, Maven |
| `chore` | Configuración, `.gitignore`, tareas menores |
| `ci` | Pipeline de integración continua |
| `revert` | Revertir un commit anterior |

### Alcances

`auth`, `db`, `ui`, `courses`, `lessons`, `admin`, `audit`, `payments`,
`progress`, `reports`, `config`, `deps`, `docs`.

## 3. Ejemplos

Bien:

```
feat(auth): registrar usuarios con contraseña cifrada

Permite crear una cuenta desde /registro. La contraseña se guarda con
BCrypt y el email se valida sin distinguir mayúsculas para evitar
cuentas duplicadas.
```

```
fix(db): impedir emails duplicados al registrarse

El índice único usaba el email tal cual, así que Ana@mail.com y
ana@mail.com entraban como cuentas distintas.
```

```
docs: documentar el flujo de arranque local

Faltaban los pasos para crear la base y ejecutar Flyway.
```

Mal:

```
cambios                      ← no dice qué ni por qué
arreglos varios               ← mezcla temas
Update index.html            ← sin tipo ni alcance
feat: todo el backend         ← commit gigante
FEAT(AUTH): LOGIN             ← mayúsculas y sin motivo
```

## 4. Reglas de higiene

1. **Antes de commitear, compilar.** Si el proyecto no compila, no se commitea.
   ```powershell
   .\mvnw.cmd -B -q -DskipTests package
   ```
2. **Revisar qué entra.** `git status` y `git diff --stat` antes de `git add`.
   Nunca `git add .` sin mirar.
3. **No mezclar temas.** Si estás arreglando el login y ves una falta de ortografía
   en la landing, son dos commits (o dos ramas).
4. **No mezclar formato con lógica.** Un reordenamiento masivo de CSS va en `style`,
   separado del `feat` que agrega una pantalla.
5. **Un commit que se pueda revertir solo**, sin arrastrar cosas no relacionadas.
6. **Nada de binarios ni secretos.** `target/`, `logs/`, `.env`,
   `application-local.yml`, contraseñas.
7. **No reescribir historia ya subida** (`--force`) salvo que se acuerde.

## 5. Qué NO se sube

Ya está en `.gitignore`:

```
target/
logs/
*.log
.env
application-local.yml
.vscode/
.idea/
```

La contraseña de la base va en variables de entorno o en un archivo local ignorado,
nunca en `application.yml` versionado.

## 6. Flujo diario

```powershell
# 1. Actualizar main
git switch main
git pull

# 2. Crear la rama del tema
git switch -c feature/luuxs-auth

# 3. Trabajar y commitear por pasos
git add src/main/java/.../model/User.java
git commit

# 4. Subir la rama
git push -u origin feature/luuxs-auth

# 5. Pull Request en GitHub y merge a main
```

## 7. Plan de commits del estado actual

El proyecto ya tiene trabajo hecho sin historial. Se divide así:

| # | Mensaje | Contenido |
|---|---|---|
| 1 | `chore: inicializar proyecto Spring Boot con PostgreSQL y Flyway` | `pom.xml`, `mvnw`, `application.yml`, clase principal, `.gitignore`, `.gitattributes` |
| 2 | `feat(db): crear esquema de cuentas y preferencias de accesibilidad` | `V1__accounts.sql` |
| 3 | `feat(ui): agregar página principal con presentación de la plataforma` | `index.html`, `layout.html`, `styles.css` |
| 4 | `feat(auth): agregar pantallas base de inicio de sesión y registro` | `AuthController`, `SecurityConfig`, `auth/*.html` |
| 5 | `docs: documentar puesta en marcha y convenciones de diseño` | `README.md`, `docs/design-system.md` |

Así cada commit cuenta una parte de la historia y el avance queda evidenciado,
que es lo que pide la rúbrica de control de versiones.

## 8. Antes de cada commit que haga el agente

1. Compila el proyecto.
2. Muestra `git diff --stat` y un resumen de lo que va a entrar.
3. Explica el **por qué** en el cuerpo del commit.
4. Commitea con el formato de arriba.
5. Nunca hace `push` sin avisar.
