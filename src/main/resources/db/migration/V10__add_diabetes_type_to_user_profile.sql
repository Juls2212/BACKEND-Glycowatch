ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS diabetes_type VARCHAR(30) NULL;

ALTER TABLE user_profile
    DROP CONSTRAINT IF EXISTS chk_user_profile_diabetes_type;

ALTER TABLE user_profile
    ADD CONSTRAINT chk_user_profile_diabetes_type
    CHECK (
        diabetes_type IS NULL
        OR diabetes_type IN ('TYPE_1', 'TYPE_2', 'PREDIABETES', 'OTHER')
    );
