package com.glycowatch.intelligence.service;

import com.glycowatch.intelligence.model.AgreementStatus;
import com.glycowatch.intelligence.model.IntelligenceConfidence;
import com.glycowatch.intelligence.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class HybridRiskResolver {

    public Resolution resolve(
            RiskLevel ruleBasedRiskLevel,
            RiskLevel externalAiRiskLevel,
            IntelligenceConfidence confidence
    ) {
        if (externalAiRiskLevel == null) {
            return new Resolution(
                    ruleBasedRiskLevel,
                    null,
                    ruleBasedRiskLevel,
                    AgreementStatus.GEMINI_UNAVAILABLE,
                    confidence
            );
        }

        RiskLevel finalRiskLevel = resolveFinalRisk(ruleBasedRiskLevel, externalAiRiskLevel);
        AgreementStatus agreementStatus = resolveAgreementStatus(ruleBasedRiskLevel, externalAiRiskLevel);

        return new Resolution(
                ruleBasedRiskLevel,
                externalAiRiskLevel,
                finalRiskLevel,
                agreementStatus,
                confidence
        );
    }

    private RiskLevel resolveFinalRisk(RiskLevel ruleBasedRiskLevel, RiskLevel externalAiRiskLevel) {
        if (ruleBasedRiskLevel == null) {
            return externalAiRiskLevel;
        }
        if (externalAiRiskLevel == null) {
            return ruleBasedRiskLevel;
        }

        if (ruleBasedRiskLevel == RiskLevel.CRITICAL || ruleBasedRiskLevel == RiskLevel.HIGH) {
            return severity(ruleBasedRiskLevel) >= severity(externalAiRiskLevel) ? ruleBasedRiskLevel : externalAiRiskLevel;
        }

        return severity(ruleBasedRiskLevel) >= severity(externalAiRiskLevel) ? ruleBasedRiskLevel : externalAiRiskLevel;
    }

    private AgreementStatus resolveAgreementStatus(RiskLevel ruleBasedRiskLevel, RiskLevel externalAiRiskLevel) {
        if (ruleBasedRiskLevel == null || externalAiRiskLevel == null) {
            return AgreementStatus.GEMINI_UNAVAILABLE;
        }
        if (ruleBasedRiskLevel == externalAiRiskLevel) {
            return AgreementStatus.FULL_AGREEMENT;
        }

        int difference = Math.abs(severity(ruleBasedRiskLevel) - severity(externalAiRiskLevel));
        return difference == 1 ? AgreementStatus.PARTIAL_AGREEMENT : AgreementStatus.DISAGREEMENT;
    }

    private int severity(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1;
            case MODERATE -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            case INSUFFICIENT_DATA -> 0;
        };
    }

    public record Resolution(
            RiskLevel ruleBasedRiskLevel,
            RiskLevel externalAiRiskLevel,
            RiskLevel finalRiskLevel,
            AgreementStatus agreementStatus,
            IntelligenceConfidence confidence
    ) {
    }
}
