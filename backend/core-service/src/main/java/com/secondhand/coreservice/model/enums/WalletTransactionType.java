package com.secondhand.coreservice.model.enums;

public enum WalletTransactionType {
    DEPOSIT,
    WITHDRAW,
    PAYMENT,
    REFUND,
    ESCROW_HOLD,     // tiền buyer bị hold khi checkout
    ESCROW_RELEASE,  // release tiền cho seller khi hoàn tất
    ESCROW_REFUND    // hoàn tiền buyer khi cancel/dispute
}
