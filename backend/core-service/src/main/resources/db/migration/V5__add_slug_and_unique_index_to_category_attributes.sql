-- Ensure category_attributes schema supports new_data seed
ALTER TABLE public.category_attributes
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

-- Backfill slug for existing rows
UPDATE public.category_attributes
SET slug = code
WHERE slug IS NULL;

-- Unique by (category_id, slug) for stable lookup/filter
CREATE UNIQUE INDEX IF NOT EXISTS uq_category_attributes_category_slug
    ON public.category_attributes(category_id, slug);
