package com.elias.automatizador.repository;

import com.elias.automatizador.model.ProcesamientoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProcesamientoLogRepository extends JpaRepository<ProcesamientoLog, Long> {
    Optional<ProcesamientoLog> findByNombreHoja(String nombreHoja);

    boolean existsByNombreHoja(String nombreHoja);

    boolean existsByNombreHojaIgnoreCase(String nombreHoja);
}
