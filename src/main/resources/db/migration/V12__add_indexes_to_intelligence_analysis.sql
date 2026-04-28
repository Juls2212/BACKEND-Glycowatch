CREATE INDEX IF NOT EXISTS idx_intelligence_analysis_user_id
    ON intelligence_analysis (user_id);

CREATE INDEX IF NOT EXISTS idx_intelligence_analysis_created_at
    ON intelligence_analysis (created_at);
