package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
                // Nếu rơi vào đây, hãy kiểm tra lại vnp_HashSecret trong file config
                return ResponseEntity.badRequest()
                        .body(new PaymentResponse("99", "Invalid signature", null, null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new PaymentResponse("99", "Error: " + e.getMessage(), null, null));
        }
    }
}