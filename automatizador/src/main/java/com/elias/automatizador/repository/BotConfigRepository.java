package com.elias.automatizador.repository;

import com.elias.automatizador.model.BotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotConfigRepository extends JpaRepository<BotConfig, Integer> {
}
