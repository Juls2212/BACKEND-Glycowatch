ALTER TABLE intelligence_analysis
    ADD COLUMN IF NOT EXISTS hypoglycemia_threshold NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS hyperglycemia_threshold NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS metrics_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS measurements_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS rule_based_analysis_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS external_ai_analysis_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS final_merged_analysis_snapshot TEXT;
