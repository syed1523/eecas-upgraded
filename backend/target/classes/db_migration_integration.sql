-- Add intelligent component fields from System B integration
ALTER TABLE expenses
ADD COLUMN IF NOT EXISTS ocr_mismatch BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS fraud_indicator BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS ocr_extracted_amount DECIMAL(19, 2),
ADD COLUMN IF NOT EXISTS ocr_extracted_merchant VARCHAR(255);
