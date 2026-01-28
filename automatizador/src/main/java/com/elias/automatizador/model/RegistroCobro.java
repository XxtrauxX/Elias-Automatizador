package com.elias.automatizador.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_cobros")
@Data
@NoArgsConstructor
public class RegistroCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nit;

    @Column(name = "nombre_empresa")
    private String nombreEmpresa;

    @Column(name = "monto_recordado")
    private Double montoRecordado;

    private String estado;

    private String canal;

    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado;
}