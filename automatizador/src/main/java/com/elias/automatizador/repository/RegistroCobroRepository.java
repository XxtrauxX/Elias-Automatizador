package com.elias.automatizador.repository;

import com.elias.automatizador.model.RegistroCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroCobroRepository extends JpaRepository<RegistroCobro, Long> {
    List<RegistroCobro> findByNit(String nit);
}