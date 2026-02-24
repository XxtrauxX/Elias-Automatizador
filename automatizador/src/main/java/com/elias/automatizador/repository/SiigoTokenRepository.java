package com.elias.automatizador.repository;

import com.elias.automatizador.model.SiigoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiigoTokenRepository extends JpaRepository<SiigoToken, Long> {
    Optional<SiigoToken> findFirstByOrderByCreatedAtDesc();
}
