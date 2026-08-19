package dev.brunohm.bv2_projeto_software_uepg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.ItemOs;

@Repository
public interface ItemOsRepository extends JpaRepository<ItemOs, Long> {
}
