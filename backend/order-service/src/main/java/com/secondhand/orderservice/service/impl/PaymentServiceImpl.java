package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.config.VnPayConfig;
import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
//import com.secondhand.orderservice.dto.request.PaymentFilterRequest;
import com.secondhand.orderservice.dto.response.AdminPaymentResponse;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.enums.PaymentMethod;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.repository.PaymentRepository;
import com.secondhand.orderservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.secret-key}")
    private String secretKey;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Override
    public PaymentResponse createVnPayPayment(CreatePaymentRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("Creating VNPay payment: amount={}, userId={}", request.getAmount(), request.getUserId());

            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String orderType = "other";
            long amount = request.getAmount() * 100;
            String bankCode = request.getBankCode();

            String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
            String vnp_IpAddr = VnPayConfig.getIpAddress(httpRequest);
            String vnp_TmnCode = tmnCode != null ? tmnCode.trim() : VnPayConfig.vnp_TmnCode;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");

            if (bankCode != null && !bankCode.isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
            vnp_Params.put("vnp_OrderType", orderType);

            String locale = request.getLanguage();
            if (locale != null && !locale.isEmpty()) {
                vnp_Params.put("vnp_Locale", locale);
            } else {
                vnp_Params.put("vnp_Locale", "vn");
            }
            vnp_Params.put("vnp_ReturnUrl", returnUrl != null ? returnUrl.trim() : VnPayConfig.vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            TimeZone vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            Calendar cld = Calendar.getInstance(vnTimeZone);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(vnTimeZone);
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Build hash data and query string
            String paymentUrl = buildVnPayUrl(vnp_Params);

            // Generate transaction ID
            String transactionId = "TXN-" + System.currentTimeMillis() + "-" + vnp_TxnRef;

            // Save payment record
            Payment payment = new Payment();
            payment.setId(UUID.randomUUID().toString());
            payment.setTransactionId(transactionId);
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setAmount((double) request.getAmount());
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return new PaymentResponse("00", "success", paymentUrl, transactionId);
        } catch (Exception e) {
            log.error("Error creating VNPay payment", e);
            return new PaymentResponse("99", "error: " + e.getMessage(), null, null);
        }
    }

    @Override
    public PaymentResponse createVnPayPaymentInternal(Long amount, String bankCode, String language, String userId, String customReturnUrl, String orderId) {
        try {
            log.info("Creating VNPay payment (internal): amount={}, userId={}, orderId={}", amount, userId, orderId);

            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String orderType = "other";
            long vnpAmount = amount * 100;
            String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
            String vnp_TmnCode = tmnCode != null ? tmnCode.trim() : VnPayConfig.vnp_TmnCode;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
            vnp_Params.put("vnp_CurrCode", "VND");

            if (bankCode != null && !bankCode.isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            // Include orderId in OrderInfo so callback can identify the order
            String orderInfo = orderId != null ? "Thanh toan don hang " + orderId : "Thanh toan don hang " + vnp_TxnRef;
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", language != null && !language.isEmpty() ? language : "vn");
            String finalReturnUrl = customReturnUrl != null && !customReturnUrl.isEmpty() ? customReturnUrl : (this.returnUrl != null ? this.returnUrl.trim() : VnPayConfig.vnp_ReturnUrl);
            vnp_Params.put("vnp_ReturnUrl", finalReturnUrl);
            vnp_Params.put("vnp_IpAddr", "0.0.0.0");

            TimeZone vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            Calendar cld = Calendar.getInstance(vnTimeZone);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(vnTimeZone);
            vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
            cld.add(Calendar.MINUTE, 15);
            vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

            // Build hash data and query string
            String paymentUrl = buildVnPayUrl(vnp_Params);

            String transactionId = "TXN-" + System.currentTimeMillis() + "-" + vnp_TxnRef;

            Payment payment = new Payment();
            payment.setId(UUID.randomUUID().toString());
            payment.setTransactionId(transactionId);
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setAmount((double) amount);
            payment.setMethod(PaymentMethod.VNPAY);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            if (orderId != null && !orderId.trim().isEmpty()) {
                com.secondhand.orderservice.model.Order orderRef = new com.secondhand.orderservice.model.Order();
                orderRef.setId(orderId);
                payment.setOrder(orderRef);
            }
            paymentRepository.save(payment);

            return new PaymentResponse("00", "success", paymentUrl, transactionId);
        } catch (Exception e) {
            log.error("Error creating VNPay payment (internal)", e);
            return new PaymentResponse("99", "error: " + e.getMessage(), null, null);
        }
    }

    @Override
    public String handleVnPayReturn(HttpServletRequest request) {
        try {
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");

            if ("00".equals(vnp_ResponseCode)) {
                // Payment successful
                return "success";
            } else {
                // Payment failed
                return "failed";
            }
        } catch (Exception e) {
            return "error";
        }
    }

    @Override
    public Boolean verifyVnPayCallback(HttpServletRequest request) {
        try {
            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                log.warn("verifyVnPayCallback - vnp_SecureHash is missing!");
                return false;
            }

            Map<String, String> fields = new HashMap<>();
            Enumeration<String> params = request.getParameterNames();

            while (params.hasMoreElements()) {
                String fieldName = params.nextElement();
                String fieldValue = request.getParameter(fieldName);

                if ((fieldValue != null) && (fieldValue.length() > 0) 
                        && !fieldName.equals("vnp_SecureHash") 
                        && !fieldName.equals("vnp_SecureHashType")) {
                    fields.put(fieldName, fieldValue);
                }
            }

            // VNPay returns params already URL-decoded via HttpServletRequest.getParameter()
            // We must re-encode them to match the hash data format used when creating the URL
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder sb = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = fields.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    sb.append(fieldName);
                    sb.append("=");
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }

            String secureKeyToUse = secretKey != null ? secretKey.trim() : VnPayConfig.secretKey;
            String vnp_SecureHashCheck = VnPayConfig.hmacSHA512(secureKeyToUse, sb.toString());

            log.info("verifyVnPayCallback - Calculated Hash: {}, VNPay Hash: {}", vnp_SecureHashCheck, vnp_SecureHash);

            return vnp_SecureHashCheck.equalsIgnoreCase(vnp_SecureHash);
        } catch (Exception e) {
            log.error("verifyVnPayCallback error", e);
            return false;
        }
    }

    @Override
    public void updatePaymentStatus(String transactionId, String status) {
        try {
            log.info("Updating payment status for transactionId: {} to status: {}", transactionId, status);

            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(PaymentStatus.valueOf(status));
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Payment status updated successfully for transactionId: {}", transactionId);
            } else {
                log.warn("Payment not found with transactionId: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("Error updating payment status", e);
        }
    }

    @Override
    public Page<AdminPaymentResponse> getAllPayments(Pageable pageable, PaymentStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        Specification<Payment> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> payments = paymentRepository.findAll(spec, pageable);
        return payments.map(this::mapToAdminPaymentResponse);
    }


    private AdminPaymentResponse mapToAdminPaymentResponse(Payment payment) {
        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .responseCode(payment.getResponseCode())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .buyerId(payment.getOrder() != null ? payment.getOrder().getBuyerId() : null)
                .build();
    }

    @Override
    public AdminPaymentResponse getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return mapToAdminPaymentResponse(payment);
    }

    /**
     * Build VNPay payment URL from params.
     * Hash data and query string both use URL-encoded values (matching VNPay official SDK).
     */
    private String buildVnPayUrl(Map<String, String> vnp_Params) {
        try {
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                    // Hash data: fieldName=encodedValue (VNPay official SDK)
                    hashData.append(fieldName).append("=").append(encodedValue);
                    // Query string: same encoding
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                         .append("=").append(encodedValue);
                }
                if (itr.hasNext()) {
                    hashData.append("&");
                    query.append("&");
                }
            }

            String secureKeyToUse = secretKey != null ? secretKey.trim() : VnPayConfig.secretKey;
            String vnp_SecureHash = VnPayConfig.hmacSHA512(secureKeyToUse, hashData.toString());
            query.append("&vnp_SecureHashType=HmacSHA512&vnp_SecureHash=").append(vnp_SecureHash);

            String payUrlToUse = payUrl != null ? payUrl.trim() : VnPayConfig.vnp_PayUrl;
            return payUrlToUse + "?" + query.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build VNPay URL", e);
        }
    }
}

