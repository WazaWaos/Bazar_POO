# Bazar 1 — Sistema de gestión comercial

Proyecto de escritorio desarrollado con Java 21, JavaFX, FXML, Maven y SQLite. Mantiene una organización por capas similar al proyecto de referencia: `controller`, `interfaces`, `implementacion`, `modelo` y `utils`.

## Módulos disponibles

- **Inicio / resumen:** acceso a las secciones principales.
- **Inventario y productos:** registro, búsqueda, actualización y eliminación de productos; control de precio y existencias.
- **Ventas y boletas:** carrito de productos, validación de stock, ventas al contado o crédito y generación de comprobante interno en TXT.
- **Clientes y deudas:** fichas persistentes, consulta de clientes, registro de deuda y abonos.
- **Caja y egresos:** ingresos cobrados, gastos registrados, saldo diario y total pendiente de cobro.

## Estructura del proyecto

- `src/main/java/.../controller`: controladores enlazados con las vistas FXML.
- `src/main/java/.../interfaces`: contratos de acceso a datos (DAO).
- `src/main/java/.../implementacion`: consultas SQL y operaciones CRUD.
- `src/main/java/.../modelo`: clases usadas por las tablas y formularios.
- `src/main/java/.../utils`: conexión SQLite, alertas y formato de moneda.
- `src/main/resources/.../views`: vistas FXML y hoja de estilos.
- `src/main/java/.../test`: clases de prueba manuales con el formato del proyecto GP1.
- `src/test/java/.../test`: pruebas automatizadas JUnit 5 para conexión, productos, clientes, deudas, ventas, caja y modelos.
- `src/main/resources/.../schema.sql`: esquema de la base (única fuente; se aplica solo al abrir la app).
- `data/bazar1.db`: base local, se crea sola en el primer arranque (no se versiona).

## Abrir en IntelliJ IDEA

1. Descomprime la carpeta.
2. En IntelliJ IDEA, abre la carpeta `Bazar1_Proyecto_Ajustado` (la que contiene `pom.xml`).
3. Configura **JDK 21** y deja que IntelliJ cargue las dependencias de Maven.
4. Ejecuta `pe.edu.upeu.project_final_gp1.HelloApplication` o desde una terminal de Windows ejecuta `mvnw.cmd clean javafx:run`.
5. Para ejecutar las pruebas JUnit 5, usa `mvnw.cmd test`.

## Base de datos y acceso

- Ruta de la base, en este orden: `-Dapp.db.path="ruta/archivo.db"`, variable de entorno `BAZAR1_DB`, o `data/bazar1.db` dentro del directorio de ejecución (se imprime en consola al arrancar). Si quieres seguir usando una base que tenías en `C:/sqlite/db/bazar1.db`, ejecuta con `-Dapp.db.path="C:/sqlite/db/bazar1.db"`.
- Si el archivo no existe se crea con el esquema actual. Si es del esquema antiguo (DNI como texto) se hace una copia `*.respaldo-esquema-antiguo` y se migra automáticamente.
- Usuario inicial: **admin / admin123** (se crea solo cuando no hay usuarios). Las contraseñas se guardan con PBKDF2; las antiguas en texto plano se convierten al primer ingreso correcto.
- DNI: 8 dígitos. Se guarda como entero y se muestra siempre con ceros a la izquierda (07654321).
- Las boletas se guardan en la carpeta `boletas/` junto a la base de datos.

La boleta que genera el programa es un comprobante interno en texto plano; no es una boleta electrónica autorizada por SUNAT.
