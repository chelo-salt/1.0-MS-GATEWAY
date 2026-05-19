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

@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    // 🛑 DEBE SER EXACTAMENTE LA MISMA CLAVE QUE USAMOS EN MS-AUTH
    private final SecretKey CLAVE_SECRETA = Keys.hmacShaKeyFor(
            "ClaveUltraSecretaEInviolableParaLaMunicipalidad2026!".getBytes()
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 🟢 Exclusión: Login pasa libre
        if (path.contains("/api/v1/auth/login")) {
            return chain.filter(exchange);
        }

        // 🕵️‍♂️ Validar presencia de la cabecera Authorization
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return rebotarPeticion(exchange, "Falta cabecera Authorization", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rebotarPeticion(exchange, "Formato de token no es Bearer", HttpStatus.UNAUTHORIZED);
        }

        String tokenPuro = authHeader.substring(7);

        try {
            // 🧠 Intentamos validar el Token de forma segura
            Jwts.parser()
                .verifyWith(CLAVE_SECRETA)
                .build()
                .parseSignedClaims(tokenPuro);
            
            // Si pasa la firma, avanzamos al microservicio correspondiente
            return chain.filter(exchange);

        } catch (Exception e) {
            // 🔥 Imprime el error real en tu consola de VS Code para saber exactamente qué falló internamente
            System.err.println("[GATEWAY ERROR] Falló la validación del Token: " + e.getMessage());
            e.printStackTrace();
            
            return rebotarPeticion(exchange, "Token invalido o vencido: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> rebotarPeticion(ServerWebExchange exchange, String mensaje, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("X-Gateway-Error", mensaje);
        System.out.println("[GATEWAY SHIELD] Acceso Denegado: " + mensaje);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Máxima prioridad
    }
}