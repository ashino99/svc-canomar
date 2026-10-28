package pe.canomar.svc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pe.canomar.svc.seguridad.ManejadorAcceso;

/**
 * Seguridad (RNF15–RNF20): contraseñas con BCrypt, acceso por rol, protección CSRF en formularios
 * y cabeceras seguras. Las rutas se limitan por rol aquí y, además, en cada controlador con @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filtro(HttpSecurity http, ManejadorAcceso manejador) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/manifest.webmanifest", "/sw.js", "/offline.html", "/error").permitAll()
                        .requestMatchers("/usuarios/**", "/productos/**", "/ajustes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/facturas/**").hasAnyRole("CONTADOR", "ADMINISTRADOR")
                        .requestMatchers("/reportes/**").hasAnyRole("CONTADOR", "ADMINISTRADOR")
                        .requestMatchers("/cobranza/**", "/ganancia/**", "/proveedores/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/vender/**", "/ventas/**", "/compras/**", "/precios/**", "/saldo/**", "/api/**").hasAnyRole("VENDEDOR", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .formLogin(f -> f.loginPage("/login").successHandler(manejador).failureHandler(manejador).permitAll())
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?salida").deleteCookies("JSESSIONID").permitAll())
                .headers(h -> h.frameOptions(fo -> fo.deny()))
                .exceptionHandling(e -> e.accessDeniedPage("/sin-permiso"));
        return http.build();
    }
}
