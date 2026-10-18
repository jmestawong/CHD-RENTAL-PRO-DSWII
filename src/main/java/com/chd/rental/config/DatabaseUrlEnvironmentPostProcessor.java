package com.chd.rental.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Heroku y Render publican la base de datos como
 * {@code DATABASE_URL=postgres://usuario:clave@host:puerto/bd}.
 * Este procesador la convierte a las propiedades JDBC de Spring
 * cuando no se definio explicitamente JDBC_DATABASE_URL.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl == null || env.getProperty("JDBC_DATABASE_URL") != null
                || !(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            return;
        }
        URI uri = URI.create(databaseUrl);
        String[] credenciales = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
        int puerto = uri.getPort() > 0 ? uri.getPort() : 5432;
        String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + puerto + uri.getPath() + query);
        if (credenciales.length > 0) {
            props.put("spring.datasource.username", credenciales[0]);
        }
        if (credenciales.length > 1) {
            props.put("spring.datasource.password", credenciales[1]);
        }
        env.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }
}
