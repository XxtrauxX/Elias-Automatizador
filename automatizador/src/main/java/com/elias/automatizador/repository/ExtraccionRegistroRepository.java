package com.elias.automatizador.repository;

import com.elias.automatizador.model.ExtraccionRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtraccionRegistroRepository extends JpaRepository<ExtraccionRegistro, Long> {
    List<ExtraccionRegistro> findByBatchId(String batchId);
}
