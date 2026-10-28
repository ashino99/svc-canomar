# Pasos para subir el proyecto a GitHub (APF3)

La rúbrica del APF3 mide **qué porcentaje de los entregables está en GitHub** (meta: 100 %), pide **al menos una rama además de `main`** y que **los dos integrantes** aparezcan en *Insights › Contributors*. Siga estos pasos en orden.

## 0. Antes de empezar
1. Cree el repositorio vacío `svc-canomar` en GitHub (sin README).
2. En *Settings › Collaborators* agregue a Coronel y a la docente.
3. Cada integrante configura su nombre una sola vez:
   ```
   git config --global user.name "Su Nombre"
   git config --global user.email "su-correo@ejemplo.com"
   ```
   Use el mismo correo de su cuenta de GitHub. Si no, sus commits no cuentan como suyos.

## 1. Rama main: documentos (Imanol)
```
cd C:\Proyectos\svc-canomar
git init
git branch -M main
git remote add origin https://github.com/SU-USUARIO/svc-canomar.git
git add README.md PASOS_GITHUB.md .gitignore docs/entregables/APF1_Equipo7.pdf docs/entregables/APF2_Equipo7.pdf
git commit -m "docs(apf1-apf2): informes de los avances 1 y 2"
git push -u origin main
git checkout -b develop
git push -u origin develop
```

## 2. Código en ramas feature, unidas a develop
Cada bloque se trabaja en su rama y se une a `develop` con un *pull request*. El otro integrante lo revisa y lo aprueba en GitHub.

**Imanol:**
```
git checkout develop
git checkout -b feature/base-modelo
git add pom.xml src/main/resources/application.properties src/main/resources/logback-spring.xml src/main/java/pe/canomar/svc/SvcCanomarApplication.java
git commit -m "build: Spring Boot, MySQL y Logback"
git add src/main/resources/schema.sql src/main/resources/data.sql src/main/java/pe/canomar/svc/modelo src/main/java/pe/canomar/svc/dao src/main/java/pe/canomar/svc/comun
git commit -m "feat(modelo): 20 tablas, vistas SQL, DAO con JdbcTemplate y cálculos"
git push -u origin feature/base-modelo
```
En GitHub: *Pull requests › New* (feature/base-modelo → develop). **Coronel** lo revisa y presiona *Merge*.

```
git checkout develop && git pull
git checkout -b feature/servicios-pantallas
git add src/main/java/pe/canomar/svc/servicio
git commit -m "feat(servicios): ventas, compras, saldo, créditos, caja, facturas y reportes"
git add src/main/java/pe/canomar/svc/web src/main/resources/templates src/main/resources/static
git commit -m "feat(pantallas): P01-P20, PWA y modo sin conexión"
git push -u origin feature/servicios-pantallas
```
Pull request a develop; Coronel lo aprueba.

**Coronel** (en su PC):
```
git clone https://github.com/SU-USUARIO/svc-canomar.git
cd svc-canomar
git checkout develop
git checkout -b feature/seguridad-tdd
git add src/main/java/pe/canomar/svc/seguridad src/main/java/pe/canomar/svc/config
git commit -m "feat(seguridad): login, roles, BCrypt y bloqueo por intentos"
git add src/test .github
git commit -m "test(tdd): 149 pruebas, una por caso de prueba de los 38 RF aptos, y GitHub Actions"
git push -u origin feature/seguridad-tdd
```
Pull request a develop; Imanol lo aprueba. Desde aquí cada push ejecuta GitHub Actions (`mvn verify`); revise que aparezca el check verde.

**Coronel:** documentos del APF3
```
git checkout develop && git pull
git checkout -b feature/docs-apf3
git add docs/entregables/APF3_Equipo7.pdf docs/entregables/Anexos_APF2_Equipo7.zip docs/anexos docs/diagramas docs/capturas docs/GUIA_DEL_CODIGO.md
git commit -m "docs(apf3): informe, anexos A-E, diagramas y capturas"
git push -u origin feature/docs-apf3
```
Pull request a develop; Imanol lo aprueba.

## 3. Entrega: develop → main
En GitHub: *Pull request* develop → main con el título "Entrega APF3". Al unirlo, cree la etiqueta `apf3` (*Releases › Create a new release*).

## 4. Capturas para el informe (capítulo 3)
- *Code › Branches* (lista de ramas) → sección 3.1.
- *Insights › Contributors* → sección 3.3.
- Un pull request aprobado y el check verde de *Actions* → sección 5.3.

Péguelas en `APF3_Equipo7.docx` donde dice **PENDIENTE DEL EQUIPO** y vuelva a exportar el PDF.

## 5. Lo que NO se sube
El **Anexo F** (`APF3_Equipo7_Anexo_F_Confidencial.pdf`) se entrega solo por UTP+Class. El `.gitignore` excluye:
- `docs/confidencial/` y los archivos `*_Confidencial*.pdf`;
- `.env` y `application-local.properties`.
