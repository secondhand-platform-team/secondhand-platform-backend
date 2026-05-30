-- Xóa ràng buộc CHECK cũ trên cột type của bảng wallet_transactions
ALTER TABLE wallet_transactions DROP CONSTRAINT IF EXISTS wallet_transactions_type_check;

-- Tạo lại ràng buộc CHECK mới chứa đầy đủ các loại giao dịch ký quỹ (Escrow)
ALTER TABLE wallet_transactions ADD CONSTRAINT wallet_transactions_type_check 
CHECK (type IN ('DEPOSIT', 'WITHDRAW', 'PAYMENT', 'REFUND', 'ESCROW_HOLD', 'ESCROW_RELEASE', 'ESCROW_REFUND'));
