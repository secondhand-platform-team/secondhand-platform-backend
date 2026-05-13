-- Add soft delete column to items table
ALTER TABLE items ADD COLUMN deleted_at TIMESTAMP NULL;

-- Create index for better query performance when filtering out deleted items
CREATE INDEX idx_items_deleted_at ON items(deleted_at);
