package dev.brunohm.bv2_projeto_software_uepg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Equipamento;

@Repository
public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
}
