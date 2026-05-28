package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderItem;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.repository.PaymentRepository;
import com.secondhand.orderservice.service.NotificationClient;
import com.secondhand.orderservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
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
            // BÆ°á»›c quan trá»ng nháº¥t: XÃ¡c thá»±c chá»¯ kÃ½ tá»« VNPay
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
                // Náº¿u rÆ¡i vÃ o Ä‘Ã¢y, hÃ£y kiá»ƒm tra láº¡i vnp_HashSecret trong file config
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
            // 1. XÃ¡c thá»±c chá»¯ kÃ½ tá»« VNPay
            Boolean isValid = paymentService.verifyVnPayCallback(request);
            if (!isValid) {
                String errorUrl = "http://localhost:3000/payment-failed?status=error&message=" +
                        URLEncoder.encode("Invalid signature from VNPay", StandardCharsets.UTF_8);
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(URI.create(errorUrl))
                        .build();
            }

            // 2. TÃ¬m Payment cÃ³ transactionId chá»©a vnp_TxnRef
            Payment payment = null;
            if (vnp_TxnRef != null) {
                List<Payment> payments = paymentRepository.findAll();
                payment = payments.stream()
                        .filter(p -> p.getTransactionId() != null && p.getTransactionId().contains(vnp_TxnRef))
                        .findFirst()
                        .orElse(null);
            }

            // 3. Kiá»ƒm tra ResponseCode (00 lÃ  thÃ nh cÃ´ng)
            if ("00".equals(vnp_ResponseCode)) {
                if (payment == null) {
                    String errorUrl = "http://localhost:3000/payment-failed?status=error&message=" +
                            URLEncoder.encode("Payment transaction not found in system", StandardCharsets.UTF_8);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                            .location(URI.create(errorUrl))
                            .build();
                }

                // Cáº­p nháº­t tráº¡ng thÃ¡i Payment
                LocalDateTime now = LocalDateTime.now();
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(now);
                paymentRepository.save(payment);

                // Cáº­p nháº­t tráº¡ng thÃ¡i Order liÃªn káº¿t
                Order order = payment.getOrder();
                if (order != null) {
                    order.setStatus(OrderStatus.PAID);
                    order.setPaymentStatus(PaymentStatus.PAID);
                    order.setUpdatedAt(now);
                    orderRepository.save(order);

                    // Náº¡p tiá»n vÃ o vÃ­ ná»™i bá»™ tá»« giao dá»‹ch VNPay
                    try {
                        walletClient.add(order.getBuyerId(), payment.getAmount(), "Náº¡p tiá»n qua VNPay cho Ä‘Æ¡n hÃ ng #" + order.getId().substring(0, 8).toUpperCase());
                        walletClient.escrowHold(order.getBuyerId(), payment.getAmount(), order.getId());
                        order.setEscrowTransactionId("ESCROW-HOLD-" + order.getId());
                        orderRepository.save(order);
                    } catch (Exception e) {
                        // ignore if already done or failed
                    }

                    // XÃ³a cÃ¡c sáº£n pháº©m Ä‘Ã£ mua khá»i giá» hÃ ng cá»§a ngÆ°á»i mua khi thanh toÃ¡n thÃ nh cÃ´ng
                    try {
                        for (OrderItem item : order.getOrderItems()) {
                            try {
                                cartService.removeItemFromCart(order.getBuyerId(), item.getItemId());
                            } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}

                    // Gá»­i thÃ´ng bÃ¡o Ä‘áº¿n ngÆ°á»i mua
                    try {
                        notificationClient.sendNotification(
                            order.getBuyerId(),
                            "ÄÆ¡n hÃ ng #" + order.getId().substring(0, 8).toUpperCase() + " Ä‘Ã£ thanh toÃ¡n thÃ nh cÃ´ng qua VNPay.",
                            "ORDER_STATUS",
                            order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
                        );
                    } catch (Exception ignored) {}

                    // Gá»­i thÃ´ng bÃ¡o Ä‘áº¿n ngÆ°á»i bÃ¡n (cÃ¡c seller cá»§a tá»«ng item)
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
                                "Sáº£n pháº©m thuá»™c Ä‘Æ¡n hÃ ng #" + order.getId().substring(0, 8).toUpperCase() + " cá»§a báº¡n Ä‘Ã£ Ä‘Æ°á»£c thanh toÃ¡n qua VNPay. Vui lÃ²ng kiá»ƒm tra vÃ  váº­n chuyá»ƒn Ä‘Ãºng háº¡n.",
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
                // Thanh toÃ¡n tháº¥t báº¡i hoáº·c bá»‹ há»§y tá»« phÃ­a khÃ¡ch hÃ ng
                if (payment != null) {
                    LocalDateTime now = LocalDateTime.now();
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);

                    Order order = payment.getOrder();
                    if (order != null) {
                        order.setStatus(OrderStatus.CANCELLED);
                        order.setPaymentStatus(PaymentStatus.FAILED);
                        order.setUpdatedAt(now);
                        orderRepository.save(order);
                    }
                }

                String errorUrl = "http://localhost:3000/payment-failed?status=error&message=" +
                        URLEncoder.encode("Payment rejected/failed by VNPay: " + vnp_ResponseCode, StandardCharsets.UTF_8);
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(URI.create(errorUrl))
                        .build();
            }
        } catch (Exception e) {
            String errorUrl = "http://localhost:3000/payment-failed?status=error&message=" +
                    URLEncoder.encode("Internal payment verification error: " + e.getMessage(), StandardCharsets.UTF_8);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
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


