CREATE TABLE IF NOT EXISTS intelligence_analysis (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rule_based_risk_level VARCHAR(50) NOT NULL,
    gemini_risk_level VARCHAR(50),
    final_risk_level VARCHAR(50) NOT NULL,
    trend VARCHAR(50) NOT NULL,
    confidence VARCHAR(50) NOT NULL,
    assistant_mood VARCHAR(50) NOT NULL,
    summary TEXT NOT NULL,
    ai_explanation TEXT NOT NULL,
    assistant_message TEXT NOT NULL,
    detected_factors TEXT NOT NULL,
    recommendations TEXT NOT NULL,
    agreement_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
