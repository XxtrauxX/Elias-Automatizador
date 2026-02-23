package com.elias.automatizador.repository;

import com.elias.automatizador.model.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {
    Optional<Contacto> findByNit(String nit);
}