// package com.secondhand.coreservice.service.impl;

// //import com.secondhand.coreservice.grpc.payment.VerifyPaymentResponse;
// import com.secondhand.coreservice.model.Item;
// import com.secondhand.coreservice.model.enums.ItemStatus;
// import com.secondhand.coreservice.repository.ItemRepository;
// import com.secondhand.coreservice.service.PaymentEventService;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.List;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class PaymentScheduler {

//     private final ItemRepository itemRepository;
//     private final PaymentEventService paymentEventService;

//     // Payment timeout: 15 minutes
//     private static final long PAYMENT_TIMEOUT_MINUTES = 15;

//     /**
//      * Check payment status for draft items every 30 seconds
//      * - If payment is verified, activate the item
//      * - If 15 minutes passed without payment, expire the item
//      */
//     @Scheduled(fixedDelay = 30000) // Run every 30 seconds
//     @Transactional
//     public void checkPaymentStatus() {
//         try {
//             log.debug("Payment scheduler running - checking draft items");

//             // Get all items with DRAFT status
//             List<Item> draftItems = itemRepository.findAllByStatus(ItemStatus.DRAFT);

//             if (draftItems.isEmpty()) {
//                 log.debug("No draft items found");
//                 return;
//             }

//             log.info("Found {} draft items to check", draftItems.size());

//             for (Item item : draftItems) {
//                 processItem(item);
//             }
//         } catch (Exception e) {
//             log.error("Error in payment scheduler", e);
//         }
//     }

//     /**
//      * Process a single draft item
//      */
//     private void processItem(Item item) {
//         try {
//             // Check if payment timeout has expired (15 minutes)
//             if (isPaymentExpired(item)) {
//                 log.warn("Payment timeout exceeded for item: {} (TransactionId: {})",
//                         item.getItemId(), item.getTransactionId());
//                 expireItem(item);
//                 return;
//             }

//             // Check if transaction ID exists
//             if (item.getTransactionId() == null || item.getTransactionId().isBlank()) {
//                 log.warn("Item {} has no transaction ID, expiring", item.getItemId());
//                 expireItem(item);
//                 return;
//             }

//             // Verify payment status with order-service
//             verifyAndActivateItem(item);
//         } catch (Exception e) {
//             log.error("Error processing item {}: {}", item.getItemId(), e.getMessage(), e);
//         }
//     }

//     /**
//      * Check if payment timeout has expired
//      */
//     private boolean isPaymentExpired(Item item) {
//         if (item.getPaymentInitiatedAt() == null) {
//             return true;
//         }

//         long minutesPassed = ChronoUnit.MINUTES.between(
//                 item.getPaymentInitiatedAt(),
//                 LocalDateTime.now());

//         return minutesPassed >= PAYMENT_TIMEOUT_MINUTES;
//     }

//     /**
//      * Verify payment and activate item if successful
//      */
//     private void verifyAndActivateItem(Item item) {
//         try {
//             log.debug("Verifying payment for item: {} (TransactionId: {})",
//                     item.getItemId(), item.getTransactionId());

//             // For now, we'll use a simple check - in production, call order-service
//             // to verify the actual payment status

//             // Check payment status by calling order-service
//             // Note: This is a simplified version - you may need to implement
//             // GetPaymentStatus properly
//             // For now, we'll assume payment is verified if responseCode is "00"

//             // In a real scenario, you'd call:
//             // VerifyPaymentResponse response =
//             // paymentEventService.getPaymentStatus(item.getTransactionId());

//             // For now, skip verification and just log
//             log.info("Payment verification skipped - would check with order-service for TransactionId: {}",
//                     item.getTransactionId());

//             // Optionally activate if you have confirmation from frontend
//             // activateItem(item);

//         } catch (Exception e) {
//             log.error("Error verifying payment for item {}: {}", item.getItemId(), e.getMessage());
//         }
//     }

//     /**
//      * Activate item after successful payment
//      */
//     public void activateItem(Item item) {
//         try {
//             log.info("Activating item: {} after successful payment", item.getItemId());

//             item.setStatus(ItemStatus.AVAILABLE);
//             item.setUpdatedAt(LocalDateTime.now());
//             item.setTransactionId(null); // Clear transaction ID after activation
//             item.setPaymentInitiatedAt(null); // Clear payment initiation timestamp

//             itemRepository.save(item);

//             log.info("Item {} activated successfully", item.getItemId());
//         } catch (Exception e) {
//             log.error("Error activating item {}: {}", item.getItemId(), e.getMessage(), e);
//         }
//     }

//     /**
//      * Expire item if payment timeout exceeded
//      */
//     private void expireItem(Item item) {
//         try {
//             log.info("Expiring item: {} due to payment timeout", item.getItemId());

//             item.setStatus(ItemStatus.EXPIRED);
//             item.setUpdatedAt(LocalDateTime.now());

//             itemRepository.save(item);

//             log.info("Item {} expired", item.getItemId());
//         } catch (Exception e) {
//             log.error("Error expiring item {}: {}", item.getItemId(), e.getMessage(), e);
//         }
//     }
// }
