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

/**
 * Genera y valida tokens JWT (HS256) firmados con la clave definida en {@code app.jwt.secret}.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionSegundos;

    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.expiracion-segundos:3600}") long expiracionSegundos) {
        this.clave = Keys.hmacShaKeyFor(bytesDeClave(secretBase64));
        this.expiracionSegundos = expiracionSegundos;
    }

    /**
     * Usa el secreto en Base64 si es valido y tiene al menos 256 bits; en otro caso
     * (por ejemplo, un valor generado por la plataforma en la nube) deriva la clave con SHA-256.
     */
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

    /** Devuelve el email (subject) si el token es valido y no ha expirado; si no, null. */
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
