ALTER TABLE glucose_measurements
    ADD COLUMN IF NOT EXISTS origin VARCHAR(20);

UPDATE glucose_measurements
SET origin = 'IOT'
WHERE origin IS NULL;

ALTER TABLE glucose_measurements
    ALTER COLUMN origin SET NOT NULL;

ALTER TABLE glucose_measurements
    ALTER COLUMN device_id DROP NOT NULL;

