package com.chd.rental.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

// Se encarga de generar el token al hacer login y de validarlo en cada peticion.
// La clave se lee de app.jwt.secret (en la nube viene de la variable JWT_SECRET).
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionSegundos;

    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.expiracion-segundos:3600}") long expiracionSegundos) {
        this.clave = Keys.hmacShaKeyFor(bytesDeClave(secretBase64));
        this.expiracionSegundos = expiracionSegundos;
    }

    // Render genera JWT_SECRET con su propio formato, por eso si no es Base64
    // valido se obtiene la clave aplicando SHA-256 al texto
    private static byte[] bytesDeClave(String secreto) {
        try {
            byte[] decodificado = Decoders.BASE64.decode(secreto);
            if (decodificado.length >= 32) {
                return decodificado;
            }
        } catch (RuntimeException ignored) {
            // no es Base64: se deriva abajo
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(secreto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String generarToken(String email, Map<String, Object> claims) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionSegundos * 1000))
                .signWith(clave)
                .compact();
    }

    // Devuelve el email del usuario si el token es valido; si esta vencido o alterado devuelve null
    public String validarYObtenerEmail(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpiracionSegundos() {
        return expiracionSegundos;
    }
}
