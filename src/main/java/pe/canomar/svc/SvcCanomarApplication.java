package pe.canomar.svc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SVC-Canomar: sistema web de ventas por kilo, créditos y cuadre diario.
 * Arquitectura MVC: controladores (web) → servicios (lógica de negocio) → DAO (acceso a datos) → MySQL.
 */
@SpringBootApplication
public class SvcCanomarApplication {
    public static void main(String[] args) {
        SpringApplication.run(SvcCanomarApplication.class, args);
    }
}
