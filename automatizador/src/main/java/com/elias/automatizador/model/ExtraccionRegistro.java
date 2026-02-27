package com.elias.automatizador.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "extracciones_lote")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraccionRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(nullable = false)
    private String nit;

    @Column(name = "monto_vencido")
    private BigDecimal montoVencido;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @PrePersist
    protected void onCreate() {
        this.fechaCarga = LocalDateTime.now();
    }
}
