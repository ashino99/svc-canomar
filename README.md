# SVC-Canomar

Sistema web de **ventas por kilo, créditos y cuadre diario** para Canomar, un negocio familiar de pescados y mariscos del Terminal Pesquero de Villa María del Triunfo.
Proyecto del Curso Integrador I: Sistemas – Software (UTP, 2026-II), Equipo 7:

- Coronel Lopez, Franco Raul
- Doria Bautista, Imanol Jeffrey

Se usa desde el navegador del celular, también instalado en la pantalla de inicio (PWA), o desde una computadora. Tiene tres roles:

- **Vendedor:** vende en el puesto.
- **Administrador del negocio:** lleva las cuentas.
- **Contador:** registra facturas y hace el cierre mensual.

## Tecnología

| Capa | Herramienta |
|---|---|
| Lenguaje y framework | Java 17+, Spring Boot 3.3 (Spring MVC) |
| Vistas | Thymeleaf, HTML y CSS adaptado al celular; JavaScript para el modo sin conexión |
| Acceso a datos | Patrón **DAO** con `JdbcTemplate` (consultas con parámetros) |
| Base de datos | MySQL 8 |
| Seguridad | Spring Security: contraseñas con **BCrypt**, roles, CSRF, bloqueo por intentos fallidos |
| Librerías de apoyo | **Google Guava**, **Apache Commons Lang**, **Apache POI** (Excel), **Logback** (registro), OpenPDF (PDF) |
| Pruebas | JUnit 5 + Mockito: 149 pruebas TDD (una por caso de prueba, paquete `tdd`) + 39 unitarias = 188 |
| Control de versiones | Git + GitHub, con GitHub Actions que compila y prueba en cada push |

## Cómo ejecutarlo

1. Instale **JDK 21**, **IntelliJ IDEA Community** y **MySQL 8**. Anote la contraseña de `root`.
2. Abra la carpeta del proyecto en IntelliJ: *File › Open*. Espere a que Maven descargue las dependencias.
3. En `src/main/resources/application.properties` cambie la contraseña de MySQL:
   `spring.datasource.password=${DB_PASSWORD:root}` → reemplace `root` por su contraseña.
4. Ejecute `SvcCanomarApplication`.
   - La base de datos `svc_canomar`, sus 20 tablas y los datos de prueba se crean solos.
5. Abra http://localhost:8080 e ingrese con alguno de estos usuarios (contraseña `Canomar2026`):

   | Usuario | Rol |
   |---|---|
   | `cuentas` | Administrador del negocio |
   | `vendedor` | Vendedor |
   | `contador` | Contador |

6. **Desde el celular:** conecte el celular al mismo Wi-Fi que la PC y abra `http://IP-de-la-PC:8080`. La IP se ve con `ipconfig`. Si no abre, permita Java en el firewall de Windows.

Pruebas: `mvn test`, o clic derecho en `src/test/java` › *Run 'All Tests'*.

**Modo sin conexión (RF24–RF26):** el navegador solo lo activa con HTTPS o en `localhost`. Para probarlo en la PC:

1. Abra http://localhost:8080/vender.
2. Presione F12 › *Network* › *Offline*.
3. Registre una venta: queda guardada en el navegador.
4. Vuelva a *Online*: se sincroniza sola y el indicador superior pasa a "Enviado".

## Estructura (MVC + DAO)

```
src/main/java/pe/canomar/svc
├── web/          Controladores (C del MVC): reciben la pantalla y llaman a los servicios
├── servicio/     Interfaces de la lógica del negocio
│   └── impl/     Implementaciones: reglas de cada RF
├── dao/          Interfaces de acceso a datos (patrón DAO)
│   └── jdbc/     Implementaciones con JdbcTemplate
├── modelo/       Datos del dominio (records)
├── comun/        Cálculos puros (importe, FIFO, costo promedio, abonos), fechas y dinero
├── seguridad/    Usuario de la sesión, carga de usuarios, bloqueo por intentos
└── config/       Configuración de seguridad
src/main/resources
├── templates/    Vistas Thymeleaf (V del MVC): una por pantalla del prototipo P01–P20
├── static/       CSS, JavaScript, service worker y manifiesto de la PWA
├── schema.sql    Las 20 tablas del modelo final y las vistas v_venta_total y v_saldo_cliente
├── data.sql      Datos de prueba
└── logback-spring.xml   Registro en consola y en logs/svc-canomar.log
```

Vea [docs/GUIA_DEL_CODIGO.md](docs/GUIA_DEL_CODIGO.md) para saber dónde está cada requerimiento y cómo se aplican MVC, DAO, SOLID, TDD y la seguridad.
