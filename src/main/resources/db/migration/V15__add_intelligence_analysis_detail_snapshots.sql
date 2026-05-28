ALTER TABLE intelligence_analysis
    ADD COLUMN IF NOT EXISTS detected_factors_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS recommendations_snapshot TEXT;
