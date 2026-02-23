package com.elias.automatizador.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_cobros")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nit;
    private String nombreCliente;
    private BigDecimal montoDeuda;
    private String canal; // WhatsApp, Email
    private String estado; // EXITOSO, FALLIDO

    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() {
        fechaEnvio = LocalDateTime.now();
    }
}