-- Adds S3 object keys while keeping the old Base64 columns nullable during migration.
-- Run the application once with the storage-migration profile to upload legacy data.
ALTER TABLE documents
    MODIFY COLUMN file_data LONGTEXT NULL,
    ADD COLUMN file_storage_key VARCHAR(500) NULL AFTER file_data;

ALTER TABLE results
    MODIFY COLUMN pdf_data LONGTEXT NULL,
    ADD COLUMN pdf_storage_key VARCHAR(500) NULL AFTER pdf_data;

-- After validating the migration and a backup, the legacy columns can be removed
-- in a later maintenance migration. Do not run this before the S3 objects exist.
