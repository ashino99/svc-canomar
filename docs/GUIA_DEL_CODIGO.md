# Guía del código de SVC-Canomar

Esta guía sirve para estudiar el proyecto antes de la sustentación. Explica cómo está organizado, dónde está cada requerimiento y cómo se cumple cada criterio de la rúbrica del APF3.

## 1. Recorrido de una petición (MVC + DAO)

Ejemplo con **RF18 Registrar venta al crédito**:

1. **Vista** `templates/vender.html` (P10). El vendedor toca un producto, ingresa los kilos y elige el restaurante. `static/js/vender.js` calcula el importe en pantalla (RF19).
2. **Controlador** `web/VentaController.registrar()` recibe el formulario (`POST /vender`). Arma un objeto `NuevaVenta` y llama al servicio. No tiene reglas del negocio.
3. **Servicio** `servicio/impl/VentaServiceImpl.registrarInterno()` aplica las reglas:
   - valida el producto, los kilos y el precio del día;
   - calcula el total;
   - consulta el crédito disponible del cliente (`CreditoService.saldo`);
   - si supera el límite: el vendedor recibe un error y el administrador debe confirmar;
   - avisa si el cliente tiene deuda vencida;
   - guarda la venta;
   - descuenta los kilos del saldo por **FIFO** (`Calculos.consumirFifo`) y guarda el costo de lo vendido.
4. **DAO** `dao/jdbc/JdbcVentaDao.insertarVenta()` ejecuta el `INSERT` con parámetros (`?`).
5. **Respuesta.**
   - Si todo sale bien, el controlador redirige con el mensaje "Venta al crédito registrada · nuevo saldo S/ …".
   - Si hay que confirmar, `web/ManejadorGlobal` muestra la pantalla `confirmar.html` con los mismos datos.
   - Si se incumple una regla, vuelve a la pantalla anterior con el mensaje de error.

## 2. Cómo se cumple la rúbrica

### Diseño de la solución (MVC, DAO, SOLID, TDD y seguridad)

| Principio | Dónde se ve |
|---|---|
| **MVC** | Vistas en `templates/`, controladores en `web/`, modelo en `modelo/` + `servicio/` |
| **DAO** | Interfaces en `dao/` (por ejemplo `VentaDao`) e implementaciones JDBC en `dao/jdbc/` (`JdbcVentaDao`). Los servicios dependen de la interfaz, nunca de SQL |
| **S** (responsabilidad única) | Cada servicio atiende un tema (ventas, créditos, caja, saldo…); los cálculos puros están en `comun/Calculos` |
| **O** (abierto/cerrado) | Para cambiar MySQL por otro motor basta otra implementación de los DAO, sin tocar los servicios |
| **L** (sustitución de Liskov) | Cualquier implementación de `VentaDao` sirve al servicio; en las pruebas se reemplaza por un *mock* |
| **I** (segregación de interfaces) | Interfaces pequeñas por tabla o tema (`AbonoDao`, `GastoDao`, `PrecioService`…) |
| **D** (inversión de dependencias) | Los servicios reciben interfaces por el constructor; Spring inyecta las implementaciones |
| **TDD** | `src/test/java/pe/canomar/svc/tdd`: una clase por RF apto (38) y un test por caso de prueba de Regla o Infraestructura (149), con el código del CP en el nombre (`rf18_cp03_…`), comentarios ARRANGE/ACT/ASSERT y DAO falsos de `TddBase`. Además quedan 39 pruebas unitarias del avance anterior. Total: 188 |
| **Seguridad** | Ver el detalle abajo |

Detalle de la seguridad:

- **Contraseñas:** se guardan con BCrypt (`SecurityConfig`).
- **Permisos por rol:** se controlan por ruta y con `@PreAuthorize` en cada controlador.
- **Bloqueo de acceso:** se bloquea 5 minutos tras 5 intentos fallidos (`ManejadorAcceso`).
- **Formularios:** tienen protección CSRF.
- **Consultas:** todas usan parámetros, para evitar la inyección SQL.
- **Redirecciones:** solo van a rutas del mismo sitio.
- **Errores:** se muestran sin detalles técnicos.
- **Bitácora:** guarda cada operación.

### Uso de recursos Java (Guava, POI, Commons, Logback)

| Librería | Uso |
|---|---|
| **Google Guava** | `Preconditions.checkArgument` e `ImmutableList` en `comun/Calculos` |
| **Apache Commons Lang** | `StringUtils` para limpiar y validar textos en servicios y DAO |
| **Apache POI** | `ReporteServiceImpl.excel*`: cierre mensual, ganancia por producto y ventas en `.xlsx` (RF48) |
| **Logback** | `logback-spring.xml`: registro en consola y en `logs/svc-canomar.log` con rotación diaria; los servicios registran ventas, compras, bloqueos y abonos |
| OpenPDF | PDF del estado de cuenta (RF37) |

### Control de versiones

- El repositorio en GitHub guarda los entregables (APF1, APF2 y anexos en `docs/`) y el código.
- Hagan commits pequeños y frecuentes, de los dos integrantes.
- GitHub Actions (`.github/workflows/maven.yml`) compila y ejecuta las pruebas en cada push; queda un check verde como evidencia.

### Interfaces gráficas (100% del alcance)

Las 20 pantallas del prototipo (P01–P20) más el menú «Más» están implementadas en `templates/`. La tabla de la sección 3 relaciona los 50 RF con su pantalla.

### Producto final

| Criterio | Cómo se cumple |
|---|---|
| Completo | Los 50 RF funcionan |
| Coherente | Las 20 tablas son las del modelo físico del APF2, y los métodos de los servicios siguen los diagramas de secuencia |
| Buenas prácticas | MVC, DAO, librerías, pruebas y Git |
| Autoría | Cada integrante debe poder explicar su parte con esta guía |

## 3. Dónde está cada requerimiento

| RF | Requerimiento | Pantalla | Ruta | Controlador | Servicio / método |
|---|---|---|---|---|---|
| RF01 | Registrar usuario | P03 | `/usuarios` | UsuarioController | UsuarioService.registrar |
| RF02 | Iniciar sesión | P01 | `/login` | SecurityConfig + ManejadorAcceso | CanomarUserDetailsService |
| RF03 | Cerrar sesión | P04 | `/mi-cuenta` | CuentaController (logout) | Spring Security |
| RF04 | Cambiar contraseña | P04 | `/mi-cuenta` | CuentaController | UsuarioService.cambiarClave |
| RF05 | Desactivar usuario | P03 | `/usuarios` | UsuarioController | UsuarioService.desactivar |
| RF06 | Registrar producto | P05 | `/productos` | ProductoController | ProductoService.registrar |
| RF07 | Editar producto | P05 | `/productos` | ProductoController | ProductoService.editar |
| RF08 | Registrar cliente | P06 | `/clientes` | ClienteController | ClienteService.registrar |
| RF09 | Editar cliente | P06 | `/clientes` | ClienteController | ClienteService.editar |
| RF10 | Registrar proveedor | P07 | `/proveedores` | ProveedorController | CompraService.registrarProveedor |
| RF11 | Registrar compra del día | P08 | `/compras/nueva` | CompraController | CompraService.registrarCompra |
| RF12 | Calcular costo promedio por kilo | P08 | `/compras/nueva` | CompraController | PrecioService.recalcularCosto |
| RF13 | Registrar pago a proveedor | P07 | `/proveedores` | ProveedorController | CompraService.registrarPago |
| RF14 | Consultar deuda con proveedores | P07 | `/proveedores` | ProveedorController | CompraService.deudas |
| RF15 | Registrar precios de venta del día | P09 | `/precios` | PrecioController | PrecioService.guardarPrecios |
| RF16 | Consultar precios del día | P09 | `/precios` | PrecioController | PrecioService.preciosDeHoy |
| RF17 | Registrar venta al contado | P10 | `/vender` | VentaController | VentaService.registrar |
| RF18 | Registrar venta al crédito | P10 | `/vender` | VentaController | VentaService.registrar (crédito) |
| RF19 | Calcular importe por peso | P10 | `/vender` | vender.js + VentaService | Calculos.importe |
| RF20 | Anular venta | P11 | `/ventas/{id}` | VentaController | VentaService.anular |
| RF21 | Registrar venta de remate | P10 | `/vender?remate=true` | VentaController | VentaService.registrar (remate) |
| RF22 | Registrar autoconsumo | P12 | `/saldo` | SaldoController | SaldoService.registrarAutoconsumo |
| RF23 | Consultar ventas del día | P11 | `/ventas` | VentaController | VentaService.ventasDelDia |
| RF24 | Guardar venta sin conexión | P10 | `/vender` | vender.js + app.js | Cola en el celular |
| RF25 | Sincronizar ventas pendientes | P10 | `/api/ventas/sync` | VentaApiController | VentaService.sincronizar |
| RF26 | Mostrar estado de sincronización | P02 | `Indicador superior` | app.js | — |
| RF27 | Registrar saldo para el día siguiente | P13 | `/saldo/cierre` | SaldoController | SaldoService.registrarCierre |
| RF28 | Consultar saldo por antigüedad | P12 | `/saldo` | SaldoController | SaldoService.saldo |
| RF29 | Alertar mercadería antigua | P12 | `/inicio` | InicioController | SaldoService.mercaderiaAntigua |
| RF30 | Registrar ajuste de saldo | P12 | `/saldo` | SaldoController | SaldoService.registrarAjuste |
| RF31 | Registrar abono de cliente | P14 | `/cobranza/{id}` | CobranzaController | CreditoService.registrarAbono |
| RF32 | Consultar saldo por cliente | P06 | `/clientes` | ClienteController | CreditoService.saldo |
| RF33 | Calcular antigüedad de la deuda | P14 | `/cobranza/{id}` | CobranzaController | CreditoService.antiguedad |
| RF34 | Alertar deudas vencidas | P14 | `/inicio, /cobranza` | InicioController | CreditoService.vencidos |
| RF35 | Registrar compromiso de pago | P14 | `/cobranza/{id}` | CobranzaController | CreditoService.registrarCompromiso |
| RF36 | Generar estado de cuenta | P15 | `/cobranza/{id}/estado` | CobranzaController | CreditoService.estadoCuenta |
| RF37 | Compartir estado de cuenta | P15 | `/cobranza/{id}/estado.pdf` | CobranzaController + app.js | ReporteService.pdfEstadoCuenta |
| RF38 | Registrar gasto menor | P16 | `/cuadre` | CajaController | CajaService.registrarGasto |
| RF39 | Realizar cuadre de caja | P16 | `/cuadre` | CajaController | CajaService.realizarCuadre |
| RF40 | Calcular ganancia del día | P17 | `/ganancia` | CajaController | CajaService.ganancia |
| RF41 | Consultar historial de cuadres | P17 | `/ganancia` | CajaController | CajaService.historial |
| RF42 | Registrar factura emitida | P18 | `/facturas` | FacturaController | FacturaService.registrar |
| RF43 | Asociar ventas a factura | P18 | `/facturas/{id}` | FacturaController | FacturaService.asociar |
| RF44 | Consultar ventas pendientes de facturar | P18 | `/facturas` | FacturaController | FacturaService.pendientes |
| RF45 | Generar cierre mensual | P19 | `/reportes` | ReporteController | ReporteService.cierreMensual |
| RF46 | Consultar panel del negocio | P02 | `/inicio` | InicioController | PanelService.panel |
| RF47 | Consultar ganancia por producto | P19 | `/reportes` | ReporteController | ReporteService.gananciaPorProducto |
| RF48 | Exportar reportes a Excel | P19 | `/reportes/excel/*` | ReporteController | ReporteService.excel* (Apache POI) |
| RF49 | Generar respaldo de la información | P20 | `/ajustes/respaldo` | AjustesController | RespaldoService.generar |
| RF50 | Consultar bitácora de auditoría | P20 | `/ajustes` | AjustesController | AuditoriaService.consultar |

## 4. Reglas de cálculo importantes (`comun/Calculos.java`)

| Regla | Fórmula | RF |
|---|---|---|
| Importe | kilos × precio, redondeado a 2 decimales | RF19 |
| Costo promedio | Σ(kilos × costo de cada lote) ÷ Σ kilos | RF12 |
| Salida del saldo | **FIFO**: se descuenta primero el lote más antiguo; con eso se calcula el costo de lo vendido, del autoconsumo y de las mermas | — |
| Saldo del cliente | ventas al crédito vigentes − abonos aplicados. No se guarda: se calcula, como se decidió en la normalización del APF2 | RF32 |
| Abonos | se aplican primero a la venta más antigua | RF31 |
| Antigüedad de la deuda | tramos 0–7, 8–15, 16–30 y más de 30 días | RF33 |
| Deuda vencida | supera el plazo del cliente | RF34 |
| Efectivo esperado del cuadre | ventas al contado en efectivo + abonos en efectivo − gastos en efectivo − pagos a proveedores en efectivo. Lo mismo para Yape y transferencias | RF39 |
| Ganancia del día | ventas − costo de lo vendido − autoconsumo, mermas y desechos − gastos | RF40 |

## 5. Preguntas probables en la sustentación

- **¿Por qué DAO y no JPA?** El curso pide el patrón DAO. Con `JdbcTemplate` el SQL queda visible y se relaciona directo con el modelo físico del APF2.
- **¿Cómo evitan duplicar una venta hecha sin señal?** Cada venta lleva un `uuid` generado en el celular, y la columna `uuid_dispositivo` es UNIQUE. Al sincronizar, las que ya llegaron se ignoran (RF25; prueba `sincronizarDuplicada`).
- **¿Por qué no se guarda el total de la venta?**
  - Porque se calcula con kilos × precio: guardarlo sería redundante (3FN).
  - El costo por kilo sí se guarda en cada línea, porque cambia cada día.
- **¿Qué pasa si el vendedor supera el límite de crédito?** Recibe un error. Solo el administrador puede autorizarlo, confirmando la operación.
