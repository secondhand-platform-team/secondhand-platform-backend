package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderItem;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.Transaction;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.model.enums.TransactionStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.repository.PaymentRepository;
import com.secondhand.orderservice.service.NotificationClient;
import com.secondhand.orderservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;
    private final com.secondhand.orderservice.service.CartService cartService;
    private final com.secondhand.orderservice.service.WalletClient walletClient;
    private final com.secondhand.orderservice.service.ItemClient itemClient;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @PostMapping("/create_payment")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {

        PaymentResponse response = paymentService.createVnPayPayment(request, httpRequest);

        if ("00".equals(response.getCode())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        try {
            // Bước quan trọng nhất: Xác thực chữ ký từ VNPay
            Boolean isValid = paymentService.verifyVnPayCallback(request);

            if (isValid) {
                String result = paymentService.handleVnPayReturn(request);
                String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
                String orderId = request.getParameter("vnp_OrderInfo");
                String amount = request.getParameter("vnp_Amount");
                String transactionId = request.getParameter("vnp_TransactionNo");

                return ResponseEntity.ok()
                        .body(new PaymentResponse("00", "Payment verified: " + result,
                                "Order: " + orderId + ", ResponseCode: " + vnp_ResponseCode, transactionId));
            } else {
                return ResponseEntity.badRequest()
                        .body(new PaymentResponse("99", "Invalid signature", null, null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new PaymentResponse("99", "Error: " + e.getMessage(), null, null));
        }
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<?> handleVNPayCallback(
            HttpServletRequest request,
            @RequestParam(required = false) String vnp_ResponseCode,
            @RequestParam(required = false) String vnp_TxnRef,
            @RequestParam(required = false) String vnp_TransactionNo) {
        try {
            log.info("VNPay callback received: vnp_ResponseCode={}, vnp_TxnRef={}, vnp_TransactionNo={}",
                    vnp_ResponseCode, vnp_TxnRef, vnp_TransactionNo);

            // 1. Xác thực chữ ký từ VNPay
            Boolean isValid = paymentService.verifyVnPayCallback(request);
            if (!isValid) {
                log.warn("VNPay callback - Invalid signature!");
                String errorUrl = buildFrontendFailedUrl("Invalid signature from VNPay");
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(URI.create(errorUrl))
                        .build();
            }

            // 2. Tìm Payment bằng vnpTxnRef (chính xác) hoặc fallback bằng transactionId contains
            Payment payment = findPaymentByVnpTxnRef(vnp_TxnRef);

            // 3. Kiểm tra ResponseCode (00 là thành công)
            if ("00".equals(vnp_ResponseCode)) {
                if (payment == null) {
                    log.error("VNPay callback - Payment not found for vnp_TxnRef: {}", vnp_TxnRef);
                    String errorUrl = buildFrontendFailedUrl("Payment transaction not found in system");
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                            .location(URI.create(errorUrl))
                            .build();
                }

                // Cập nhật trạng thái Payment
                LocalDateTime now = LocalDateTime.now();
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(now);
                payment.setResponseCode(vnp_ResponseCode);
                paymentRepository.save(payment);
                log.info("Payment {} status updated to PAID", payment.getId());

                // Tạo Transaction entity
                Transaction transaction = new Transaction();
                transaction.setId(UUID.randomUUID().toString());
                transaction.setTransactionCode(vnp_TransactionNo != null ? vnp_TransactionNo : vnp_TxnRef);
                transaction.setAmount(payment.getAmount());
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setCreatedAt(now);
                transaction.setPayment(payment);
                payment.setTransaction(transaction);
                paymentRepository.save(payment);
                log.info("Transaction {} created for payment {}", transaction.getId(), payment.getId());

                // Cập nhật trạng thái Order liên kết
                Order order = payment.getOrder();
                if (order != null) {
                    order.setStatus(OrderStatus.PAID);
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setUpdatedAt(now);
                    orderRepository.save(order);
                    log.info("Order {} status updated to PAID", order.getId());

                    // Nạp tiền vào ví nội bộ từ giao dịch VNPay
                    try {
                        walletClient.add(order.getBuyerId(), payment.getAmount(), "Nạp tiền qua VNPay cho đơn hàng #" + order.getId().substring(0, 8).toUpperCase());
                        walletClient.escrowHold(order.getBuyerId(), payment.getAmount(), order.getId());
                        order.setEscrowTransactionId("ESCROW-HOLD-" + order.getId());
                        orderRepository.save(order);
                        log.info("Escrow hold successful for order {}", order.getId());
                    } catch (Exception e) {
                        log.warn("Escrow hold failed for order {}: {}", order.getId(), e.getMessage());
                    }

                    // Xóa các sản phẩm đã mua khỏi giỏ hàng của người mua khi thanh toán thành công
                    try {
                        for (OrderItem item : order.getOrderItems()) {
                            try {
                                cartService.removeItemFromCart(order.getBuyerId(), item.getItemId());
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}

                    // Gửi thông báo đến người mua
                    try {
                        notificationClient.sendNotification(
                            order.getBuyerId(),
                            "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đã thanh toán thành công qua VNPay.",
                            "ORDER_STATUS",
                            order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
                        );
                    } catch (Exception ignored) {}

                    // Gửi thông báo đến người bán (các seller của từng item)
                    try {
                        java.util.Set<String> sellerIds = new java.util.HashSet<>();
                        for (OrderItem item : order.getOrderItems()) {
                            if (item.getSellerId() != null) {
                                sellerIds.add(item.getSellerId());
                            }
                        }
                        for (String sellerId : sellerIds) {
                            String itemId = order.getOrderItems().stream()
                                .filter(i -> sellerId.equals(i.getSellerId()))
                                .map(OrderItem::getItemId)
                                .findFirst()
                                .orElse(null);

                            notificationClient.sendNotification(
                                sellerId,
                                "Sản phẩm thuộc đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " của bạn đã được thanh toán qua VNPay. Vui lòng kiểm tra và vận chuyển đúng hạn.",
                                "ORDER_CREATED",
                                itemId
                            );
                        }
                    } catch (Exception ignored) {}
                }

                String successUrl = buildFrontendSuccessUrl(vnp_TransactionNo);
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(URI.create(successUrl))
                        .build();
            } else {
                // Thanh toán thất bại hoặc bị hủy từ phía khách hàng
                log.info("VNPay callback - Payment failed/cancelled: responseCode={}", vnp_ResponseCode);
                if (payment != null) {
                    LocalDateTime now = LocalDateTime.now();
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setResponseCode(vnp_ResponseCode);
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    if (order != null) {
                        order.setStatus(OrderStatus.CANCELLED);
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        order.setUpdatedAt(now);
                        orderRepository.save(order);

                        try {
                            for (OrderItem item : order.getOrderItems()) {
                                itemClient.updateItemStatus(item.getItemId(), "ACTIVE");
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }

                String errorUrl = buildFrontendFailedUrl("Payment rejected/failed by VNPay: " + vnp_ResponseCode);
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(URI.create(errorUrl))
                        .build();
            }
        } catch (Exception e) {
            log.error("VNPay callback error", e);
            String errorUrl = buildFrontendFailedUrl("Internal payment verification error: " + e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }

    /**
     * Tìm Payment bằng vnpTxnRef.
     * Ưu tiên lookup bằng field vnpTxnRef (chính xác).
     * Fallback: tìm trong transactionId chứa vnpTxnRef.
     */
    private Payment findPaymentByVnpTxnRef(String vnpTxnRef) {
        if (vnpTxnRef == null || vnpTxnRef.isEmpty()) {
            return null;
        }

        // Ưu tiên: lookup bằng vnpTxnRef field
        Payment payment = paymentRepository.findByVnpTxnRef(vnpTxnRef).orElse(null);
        if (payment != null) {
            log.info("Found payment by vnpTxnRef: {}", payment.getId());
            return payment;
        }

        // Fallback: tìm trong transactionId (format: TXN-{timestamp}-{vnpTxnRef})
        List<Payment> payments = paymentRepository.findByTransactionIdContaining(vnpTxnRef);
        if (!payments.isEmpty()) {
            payment = payments.stream()
                    .max(java.util.Comparator.comparing(Payment::getCreatedAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                    .orElse(null);
            if (payment != null) {
                log.info("Found payment by transactionId containing '{}': {}", vnpTxnRef, payment.getId());
            }
        }

        return payment;
    }

    private String buildFrontendSuccessUrl(String transactionId) {
        return frontendBaseUrl + "/payment-success?status=success&transactionId=" +
                URLEncoder.encode(transactionId == null ? "" : transactionId, StandardCharsets.UTF_8);
    }

    private String buildFrontendFailedUrl(String message) {
        return frontendBaseUrl + "/payment-failed?status=error&message=" +
                URLEncoder.encode(message == null ? "Unknown error" : message, StandardCharsets.UTF_8);
    }
}