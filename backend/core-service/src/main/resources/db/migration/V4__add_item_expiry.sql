-- =============================================
-- V4: Thêm cột expired_at vào bảng items
-- Hỗ trợ hệ thống hết hạn & gia hạn tin đăng:
--   - Tin miễn phí (FREE_SELL/GIVE_AWAY): hết hạn sau 5 ngày
--   - Tin tính phí (SELL): hết hạn sau 15 ngày
-- =============================================

ALTER TABLE public.items ADD COLUMN expired_at TIMESTAMP(6) WITHOUT TIME ZONE;

-- Index để scheduler tìm nhanh các tin hết hạn
CREATE INDEX idx_items_expired_at ON public.items USING btree (expired_at);
