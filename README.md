# 🎛️ ms-gateway | API Gateway Perimetral Reactivo

Este módulo actúa como la **Puerta Única de Entrada (Single Entry Point)** de toda la arquitectura de microservicios del Complejo Deportivo Municipal. Construido sobre **Spring Cloud Gateway** y motorizado por **Netty**, centraliza el tráfico externo bajo el puerto `8090` e intercepta los predicados de red para enrutar las peticiones al puerto interno correspondiente.

---

## 🛠️ Stack Tecnológico Core
* **Lenguaje:** Java 21 LTS
* **Framework:** Spring Boot 3.4.3 (Estable e integrado)
* **Sub-Framework:** Spring Cloud Gateway (2024.0.0)
* **Servidor de Red:** Netty (Arquitectura No-Bloqueante y Reactiva vía WebFlux)
* **Persistencia:** Ninguna (Componente de infraestructura puro, desacoplado de bases de datos)

---

## 📂 Mapa de Enrutamiento Centralizado (Puerto 8090)

El Gateway expone una sola IP y distribuye de forma transparente según el prefijo de la URI:

| Microservicio | Puerto Interno | Prefijo de Ruta Interceptado |
| :--- | :---: | :--- |
| `ms-auth` | `8080` | `/api/v1/auth/**` |
| `ms-canchas` | `8081` | `/api/v1/canchas/**` |
| `ms-pagos` | `8082` | `/api/v1/pagos/**` |
| `ms-reservas` | `8083` | `/api/v1/reservas/**` |
| `ms-disponibilidad` | `8084` | `/api/v1/disponibilidad/**` |
| `ms-restricciones` | `8085` | `/api/v1/restricciones/**` |
| `ms-notificaciones` | `8086` | `/api/v1/notificaciones/**` |
| `ms-reportes` | `8087` | `/api/v1/reportes/**` |
| `ms-soporte` | `8088` | `/api/v1/soporte/**` |

---

## 🧪 Verificación de Enrutamiento Integrado (Postman Test)
* **URL Centralizada:** `POST http://localhost:8090/api/v1/reportes/generar`
* **Carga Útil (JSON):**
```json
{
    "tipoReporte": "Financiero",
    "fechaInicio": "2026-05-01",
    "fechaFin": "2026-05-31",
    "generadoPor": "Chelo_El_Arquitecto"
}
Respuesta Exitosa: Transfiere la carga a la capa de inteligencia transaccional de manera asíncrona, devolviendo un estatus 200 OK con el balance unificado.