package com.chd.rental.config;

import com.chd.rental.entity.Cliente;
import com.chd.rental.entity.Usuario;
import com.chd.rental.entity.Vehiculo;
import com.chd.rental.entity.enums.RolUsuario;
import com.chd.rental.repository.ClienteRepository;
import com.chd.rental.repository.UsuarioRepository;
import com.chd.rental.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Al iniciar la aplicacion crea usuarios, clientes y vehiculos de ejemplo
// solo si las tablas estan vacias. Las contrasenas se guardan con BCrypt.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.default-password}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.saveAll(List.of(
                    usuario("Administrador CH&D", "admin@chdrental.com", adminPassword, RolUsuario.ADMIN),
                    usuario("Operador de Flota", "operador@chdrental.com", defaultPassword, RolUsuario.OPERADOR),
                    usuario("Responsable de Pagos", "pagos@chdrental.com", defaultPassword, RolUsuario.PAGOS),
                    usuario("Gerencia General", "gerencia@chdrental.com", defaultPassword, RolUsuario.GERENCIA)));
            log.info("Usuarios iniciales creados (contrasenas cifradas con BCrypt)");
        }
        if (clienteRepository.count() == 0) {
            clienteRepository.saveAll(List.of(
                    Cliente.builder().razonSocial("Minera Andina del Norte S.A.C.").ruc("20512345671")
                            .sector("mineria").direccion("Av. Luzuriaga 450, Huaraz").telefono("043421100")
                            .email("logistica@mineraandina.pe").contactoNombre("Rosa Caldas")
                            .contactoCargo("Jefa de Logistica").contactoTelefono("987654321").build(),
                    Cliente.builder().razonSocial("Constructora Cordillera Blanca S.A.").ruc("20698765432")
                            .sector("construccion").direccion("Jr. San Martin 120, Caraz").telefono("043391200")
                            .email("operaciones@cordillerablanca.pe").contactoNombre("Luis Huerta")
                            .contactoCargo("Residente de Obra").contactoTelefono("956111222").build(),
                    Cliente.builder().razonSocial("Agroexport Santa Rosa E.I.R.L.").ruc("20455667788")
                            .sector("agricultura").direccion("Carretera Casma km 5").telefono("043411900")
                            .email("admin@agrosantarosa.pe").contactoNombre("Ana Valverde")
                            .contactoCargo("Administradora").contactoTelefono("944333555").build()));
        }
        if (vehiculoRepository.count() == 0) {
            vehiculoRepository.saveAll(List.of(
                    vehiculo("ABC-123", "Toyota", "Hilux", 2023, "4x4 pickup", "Blanco", 42000, "250.00"),
                    vehiculo("BCD-456", "Nissan", "Frontier", 2022, "4x4 pickup", "Gris", 58000, "230.00"),
                    vehiculo("CDE-789", "Mitsubishi", "L200", 2024, "4x4 pickup", "Rojo", 15000, "260.00"),
                    vehiculo("DEF-321", "Toyota", "Land Cruiser", 2021, "4x4 suv", "Negro", 76000, "380.00")));
        }
    }

    private Usuario usuario(String nombre, String email, String clave, RolUsuario rol) {
        return Usuario.builder()
                .nombreCompleto(nombre)
                .email(email)
                .password(passwordEncoder.encode(clave))
                .rol(rol)
                .build();
    }

    private Vehiculo vehiculo(String placa, String marca, String modelo, int anio, String tipo, String color,
                              int km, String tarifa) {
        return Vehiculo.builder()
                .placa(placa).marca(marca).modelo(modelo).anio(anio).tipo(tipo).color(color)
                .capacidadPasajeros(5).capacidadCarga(new BigDecimal("1.00"))
                .kilometraje(km).tarifaDiaria(new BigDecimal(tarifa))
                .fechaSoat(LocalDate.now().plusMonths(8))
                .fechaRevisionTecnica(LocalDate.now().plusMonths(5))
                .build();
    }
}
