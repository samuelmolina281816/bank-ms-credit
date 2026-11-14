# bank-ms-credit

Microservicio de créditos de la banca. La implementación actual cubre el dominio y contrato de la Fase I; no implementa capacidades de las Fases II y III.

## Arquitectura

El servicio sigue arquitectura hexagonal:

- `domain`: modelos y reglas independientes de Spring y PostgreSQL.
- `application`: casos de uso y puertos de entrada/salida.
- `framework/adapter/input/rest`: implementaciones de las interfaces generadas desde OpenAPI.
- `framework/adapter/output`: adaptadores de persistencia, secuencias y validación de Customer.

El contrato canónico es `src/main/resources/properties/openapi.yaml`. Maven genera modelos e interfaces reactivas con OpenAPI Generator. Los adaptadores REST implementan esas interfaces; SpringDoc documenta los handlers registrados y Swagger UI presenta el contrato.

## PostgreSQL frente a MongoDB

`requirements.md` propone MongoDB. Para este microservicio se adopta PostgreSQL como excepción autorizada y por las características transaccionales del crédito:

- Un pago debe actualizar la deuda y agregar su movimiento de forma atómica.
- Restricciones `CHECK`, `UNIQUE`, FK local e índice único parcial protegen invariantes incluso ante solicitudes concurrentes.
- `credit_movements` es un historial inmutable, mientras `credits` conserva el estado vigente.
- La relación entre crédito y movimiento es fija y requiere consistencia referencial.

MongoDB puede ejecutar transacciones, pero el esquema relacional y las restricciones declarativas de PostgreSQL ajustan mejor este flujo y hacen explícita la integridad financiera. Cada microservicio conserva su propia base: `customer_number` es una referencia funcional y no una FK a Customer.

No se utiliza `@Query`. Los repositorios de Spring Data R2DBC usan métodos derivados; el adaptador de contadores ejecuta sentencias PostgreSQL estáticas y parametrizadas para asignar secuencias diarias concurrentes.

## Persistencia

El DDL inicial se encuentra en `src/main/resources/schema.sql` y se aplica mediante la inicialización SQL R2DBC de Spring Boot.

| Tabla | Responsabilidad |
|---|---|
| `credits` | Estado actual del crédito, cliente, tipo, deuda y auditoría. |
| `credit_movements` | Desembolsos y pagos inmutables, con deuda resultante e idempotencia. |
| `credit_code_counters` | Contadores técnicos por fecha y tipo para generar identificadores sin colisiones entre instancias. No representa una entidad de negocio. |

El pago y su movimiento se confirman juntos. El campo `version` permite control optimista en la actualización del crédito. El índice parcial impide más de un crédito personal `ACTIVE` por cliente. Al evolucionar el esquema en las siguientes fases, se debe migrar a Flyway/Liquibase para versionar cambios incrementales.

## Configuración

Valores locales predeterminados: servicio en `8083`, Customer en `8080`, Account en `8081`, Card en `8082` y PostgreSQL local en el puerto `5434`. La configuración admite externalización:

| Variable | Uso | Valor local predeterminado |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP | `8083` |
| `CREDIT_R2DBC_URL` | URL R2DBC de la base propia | `r2dbc:postgresql://localhost:5434/bank_credit_dev` |
| `CREDIT_DB_USERNAME` | Usuario de la base propia (también almacenado en Vault) | Sin valor predeterminado |
| `CREDIT_DB_PASSWORD` | Clave de la base propia (también almacenada en Vault) | Sin valor predeterminado |
| `CREDIT_SCHEMA_INIT_MODE` | Modo de inicialización de `schema.sql` | `always` |
| `CONFIG_SERVER_URI` | URI de Config Server | `http://localhost:8888` |
| `CONFIG_SERVER_ENABLED` | Habilita Config Server | `true` |
| `VAULT_ADDR` | URI de HashiCorp Vault | `http://127.0.0.1:8200` |
| `VAULT_TOKEN` | Token local de Vault | Sin valor predeterminado |
| `CUSTOMER_ELIGIBILITY_URL` | Prefijo del endpoint de elegibilidad de Customer | `http://localhost:8080/api/v1/customer/documents` |
| `CUSTOMER_TIMEOUT` | Timeout del cliente HTTP Customer | `2s` |

La aplicación importa el archivo `.env` de la raíz como configuración local. Completa ese archivo (o copia `.env.example` a `.env`) con las credenciales de la base Credit y el token de Vault, y no agregues `.env` al repositorio. Cuando Vault esté disponible, el secreto KV se lee de `secret/banca-project/bank-ms-credit`.

En Git Bash, crea o actualiza el secreto con `bash scripts/vault-crear-secrets.sh`. El script usa `/c/Dev/Software/vault/vault.exe` de forma predeterminada; si Vault está instalado en otra ruta, configura `VAULT_EXE` antes de ejecutarlo. La inicialización de esquema está habilitada para desarrollo local.

La creación y búsqueda por documento consultan `GET {CUSTOMER_ELIGIBILITY_URL}/{documentType}/{documentNumber}/eligibility`, según el contrato de `bank-ms-customer`. Se valida el `customerNumber`, el tipo (`PERSONAL`/`BUSINESS`) y el indicador `eligible`; Customer mantiene la propiedad de sus datos y no se consulta su base desde Credit. Los errores 400/404/409/422 y la indisponibilidad del servicio se traducen a las respuestas definidas para la API de Credit.

## API y ejecución

Swagger UI: `http://localhost:8083/swagger-ui/index.html`  
Contrato servido: `http://localhost:8083/openapi.yaml`  
OpenAPI generado por SpringDoc: `http://localhost:8083/v3/api-docs`

Handlers REST contract-first publicados (las operaciones que requieren Customer solo podrán completar el flujo cuando se configure su contrato real):

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/credit` | Crear crédito y desembolso inicial |
| `GET` | `/api/v1/credit` | Listar o filtrar por cliente/documento |
| `GET` | `/api/v1/credit/{creditNumber}` | Consultar crédito |
| `PUT` | `/api/v1/credit/{creditNumber}` | Actualizar estado permitido |
| `DELETE` | `/api/v1/credit/{creditNumber}` | Cerrar lógicamente |
| `GET` | `/api/v1/credit/{creditNumber}/outstanding-debt` | Consultar deuda |
| `GET` | `/api/v1/credit/{creditNumber}/movements` | Consultar movimientos |
| `POST` | `/api/v1/credit/{creditNumber}/payments` | Registrar pago con `Idempotency-Key` |

Compilar y probar:

```powershell
.\mvnw.cmd test
```

La colección de pruebas Postman se encuentra en `postman/bank-ms-credit-phase-i.postman_collection.json`. Las operaciones que dependen de PostgreSQL requieren la base configurada y disponible. La creación y búsqueda por documento requieren además que `bank-ms-customer` esté disponible en el endpoint configurado.

## Evolución

Las siguientes fases podrán agregar nuevas estrategias, transferencias/reportes, cuotas y mora, eventos Kafka/outbox, JWT, caché y Yanki sin permitir acceso cruzado a las bases. No se crean ahora tablas de cuotas ni eventos porque esas funcionalidades están fuera de Fase I.
