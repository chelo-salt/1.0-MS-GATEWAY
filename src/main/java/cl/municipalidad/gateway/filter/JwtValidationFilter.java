package cl.municipalidad.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

/**
 * Filtro Global del Gateway. Actúa como el "Escudo Protector" de la arquitectura.
 * Intercepta absolutamente todas las peticiones que entran por el puerto 8090
 * para validar si el usuario tiene un pase (Token JWT) válido antes de dejarlo pasar.
 */
@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    // ⚠️ REGLA DE ORO: Esta firma matemática DEBE ser idéntica a la que usas en 'ms-auth'.
    // Si cambia un solo carácter, el Gateway no podrá descifrar los tokens y rebotará todo.
    private final SecretKey CLAVE_SECRETA = Keys.hmacShaKeyFor(
            "ClaveUltraSecretaEInviolableParaLaMunicipalidad2026!".getBytes()
    );

    /**
     * Método principal que intercepta la petición y decide si pasa o se bloquea.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 🟢 EXCEPCIÓN DE SEGURIDAD (Zona Libre):
        // Si el usuario está intentando hacer Login, lógicamente no tiene token todavía.
        // El Gateway detecta la ruta de autenticación y lo deja pasar libremente hacia 'ms-auth'.
        if (path.contains("/api/v1/auth/login")) {
            return chain.filter(exchange);
        }

        // 🕵️‍♂️ PASO 1: Validar si la petición trae la cabecera de seguridad obligatoria
        // Si no existe la cabecera "Authorization", la petición se rechaza de inmediato.
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return rebotarPeticion(exchange, "Falta cabecera Authorization", HttpStatus.UNAUTHORIZED);
        }

        // Obtener el texto completo de la cabecera (Ej: "Bearer eyJhbGciOi...")
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        // 🕵️‍♂️ PASO 2: Validar el formato estándar de la cabecera
        // Debe empezar estrictamente con la palabra "Bearer " (que significa 'portador' del token)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rebotarPeticion(exchange, "Formato de token no es Bearer", HttpStatus.UNAUTHORIZED);
        }

        // ✂️ PASO 3: Extraer el token puro
        // Cortamos los primeros 7 caracteres ("Bearer ") para quedarnos únicamente con el String largo del JWT
        String tokenPuro = authHeader.substring(7);

        try {
            // 🧠 PASO 4: La Prueba de Fuego (Validación Matemática)
            // La librería intenta abrir el token usando nuestra CLAVE_SECRETA.
            // Si el token fue inventado, alterado o ya caducó por tiempo, aquí saltará una excepción.
            Jwts.parser()
                .verifyWith(CLAVE_SECRETA)
                .build()
                .parseSignedClaims(tokenPuro);
            
            // ✅ ¡ÉXITO! Si la firma es auténtica, el semáforo se pone en verde
            // e instruye al Gateway a redirigir la petición al microservicio final (Pagos, Canchas, etc.)
            return chain.filter(exchange);

        } catch (Exception e) {
            // 🛑 TRATAMIENTO DE ERRORES:
            // Si el token falló, imprimimos el motivo exacto en la consola de VS Code para el desarrollador.
            System.err.println("[GATEWAY ERROR] Falló la validación del Token: " + e.getMessage());
            e.printStackTrace();
            
            // Devolvemos un código 401 Unauthorized a Postman/Frontend indicando que el token no sirve.
            return rebotarPeticion(exchange, "Token invalido o vencido: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Método auxiliar encargado de armar la respuesta de rechazo (Baneo/Rebote).
     * Devuelve un estado HTTP 401 (No autorizado) y agrega una cabecera personalizada con el motivo.
     */
    private Mono<Void> rebotarPeticion(ServerWebExchange exchange, String mensaje, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        
        // Añade una cabecera de diagnóstico útil para revisar en Postman qué regla falló exactamente
        response.getHeaders().add("X-Gateway-Error", mensaje);
        
        System.out.println("[GATEWAY SHIELD] Acceso Denegado: " + mensaje);
        return response.setComplete(); // Corta el flujo de la petición aquí mismo.
    }

    /**
     * Define la prioridad del filtro en la cadena de Spring Cloud Gateway.
     * Retornar -1 (prioridad máxima) garantiza que la seguridad se ejecute ANTES de cualquier
     * otra lógica de enrutamiento o manipulación de datos.
     */
    @Override
    public int getOrder() {
        return -1; 
    }
}