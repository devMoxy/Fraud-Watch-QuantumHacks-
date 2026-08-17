package com.fraudwatch.repository;

import com.fraudwatch.model.AnomalyFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyFlagRepository extends JpaRepository<AnomalyFlag, Long> {

    List<AnomalyFlag> findTop100ByOrderByFlaggedAtDesc();
}
