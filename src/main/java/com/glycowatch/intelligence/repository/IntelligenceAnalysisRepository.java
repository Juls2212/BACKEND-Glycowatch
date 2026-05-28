package com.glycowatch.intelligence.repository;

import com.glycowatch.intelligence.model.IntelligenceAnalysis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntelligenceAnalysisRepository extends JpaRepository<IntelligenceAnalysis, Long> {

    List<IntelligenceAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<IntelligenceAnalysis> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<IntelligenceAnalysis> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<IntelligenceAnalysis> findByIdAndUserId(Long id, Long userId);
}
