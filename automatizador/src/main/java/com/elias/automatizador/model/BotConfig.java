package com.elias.automatizador.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_bot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fila_inicio")
    private Integer filaInicio;

    @Column(name = "columna_inicio")
    private String columnaInicio;

    @Column(name = "columna_fin")
    private String columnaFin;

    @Column(name = "hoja_nombre")
    private String hojaNombre;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;
}
