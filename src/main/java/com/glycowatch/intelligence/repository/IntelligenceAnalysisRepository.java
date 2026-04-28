package com.glycowatch.intelligence.repository;

import com.glycowatch.intelligence.model.IntelligenceAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntelligenceAnalysisRepository extends JpaRepository<IntelligenceAnalysis, Long> {

    List<IntelligenceAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
