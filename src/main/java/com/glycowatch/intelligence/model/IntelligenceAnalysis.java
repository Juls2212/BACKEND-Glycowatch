package com.glycowatch.intelligence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "intelligence_analysis")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rule_based_risk_level", nullable = false, length = 50)
    private String ruleBasedRiskLevel;

    @Column(name = "gemini_risk_level", length = 50)
    private String geminiRiskLevel;

    @Column(name = "final_risk_level", nullable = false, length = 50)
    private String finalRiskLevel;

    @Column(name = "trend", nullable = false, length = 50)
    private String trend;

    @Column(name = "confidence", nullable = false, length = 50)
    private String confidence;

    @Column(name = "assistant_mood", nullable = false, length = 50)
    private String assistantMood;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "ai_explanation", nullable = false, columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "assistant_message", nullable = false, columnDefinition = "TEXT")
    private String assistantMessage;

    @Column(name = "detected_factors", nullable = false, columnDefinition = "TEXT")
    private String detectedFactors;

    @Column(name = "recommendations", nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "agreement_status", nullable = false, length = 50)
    private String agreementStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
