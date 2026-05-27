package com.secondhand.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderEvent;
import com.secondhand.orderservice.model.enums.OrderEventType;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Event Sourcing — Event Store
 * 
 * Lưu trữ và truy vấn chuỗi events cho mỗi order.
 * 
 * Core concepts:
 * - recordEvent(): Append 1 event mới (immutable)
 * - getEventsByOrderId(): Đọc toàn bộ event stream
 * - rebuildOrderState(): Replay events → rebuild state (Event Sourcing core)
 * 
 * Lợi ích so với chỉ lưu current state:
 * - Biết chính xác "ai đã làm gì, khi nào" (audit)
 * - Có thể rollback / replay
 * - Debug production issues dễ dàng hơn
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventStore {

    private final OrderEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Ghi 1 event vào event store.
     * Mỗi event chứa:
     * - orderId: đơn hàng liên quan
     * - eventType: loại sự kiện
     * - actorId: ai thực hiện
     * - actorRole: vai trò (BUYER, SELLER, SYSTEM, ADMIN)
     * - metadata: dữ liệu bổ sung (JSON)
     * - snapshot: trạng thái order hiện tại (JSON)
     */
    public void recordEvent(String orderId, OrderEventType eventType,
                            String actorId, String actorRole, 
                            Map<String, Object> metadata, Order snapshot) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .eventType(eventType)
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .metadata(metadata != null ? objectMapper.writeValueAsString(metadata) : null)
                    .snapshotJson(buildSnapshot(snapshot))
                    .createdAt(LocalDateTime.now())
                    .build();

            eventRepository.save(event);
            log.info("[EventStore] Recorded: orderId={}, type={}, actor={}/{}", 
                    orderId, eventType, actorRole, actorId);
        } catch (Exception e) {
            // Event recording failure KHÔNG nên block business logic
            log.error("[EventStore] Failed to record event: orderId={}, type={}, error={}", 
                    orderId, eventType, e.getMessage(), e);
        }
    }

    /**
     * Lấy toàn bộ events của 1 order (timeline).
     */
    public List<OrderEvent> getEventsByOrderId(String orderId) {
        return eventRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    /**
     * Event Sourcing Core — Rebuild order state từ event stream.
     * 
     * Replay tất cả events theo thứ tự thời gian → tính ra trạng thái cuối cùng.
     * Dùng để verify consistency hoặc debug.
     */
    public Map<String, Object> rebuildOrderState(String orderId) {
        List<OrderEvent> events = getEventsByOrderId(orderId);
        
        if (events.isEmpty()) {
            log.warn("[EventStore] No events found for orderId={}", orderId);
            return Collections.emptyMap();
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("orderId", orderId);
        state.put("currentStatus", "UNKNOWN");
        state.put("eventCount", events.size());

        // Replay: áp dụng từng event lên state
        for (OrderEvent event : events) {
            switch (event.getEventType()) {
                case ORDER_CREATED -> state.put("currentStatus", "PENDING_PAYMENT");
                case ORDER_PAID -> state.put("currentStatus", "PAID");
                case ORDER_PREPARING -> state.put("currentStatus", "PREPARING");
                case ORDER_HANDOVER -> state.put("currentStatus", "HANDOVER_TO_SHIPPER");
                case ORDER_IN_TRANSIT -> state.put("currentStatus", "IN_TRANSIT");
                case ORDER_DELIVERED -> state.put("currentStatus", "DELIVERED");
                case ORDER_COMPLETED, ORDER_AUTO_COMPLETED -> state.put("currentStatus", "COMPLETED");
                case ORDER_CANCELLED -> state.put("currentStatus", "CANCELLED");
                case ORDER_DISPUTED -> state.put("currentStatus", "DISPUTED");
                case ORDER_DISPUTE_RESOLVED -> {
                    // Metadata chứa action (refund/release)
                    state.put("currentStatus", "RESOLVED");
                }
                default -> {} // Escrow events không thay đổi order status
            }
            state.put("lastEvent", event.getEventType().name());
            state.put("lastEventAt", event.getCreatedAt().toString());
            state.put("lastActor", event.getActorId());
        }

        // Timeline summary
        List<Map<String, String>> timeline = new ArrayList<>();
        for (OrderEvent event : events) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("type", event.getEventType().name());
            entry.put("actor", event.getActorRole() + ":" + event.getActorId());
            entry.put("at", event.getCreatedAt().toString());
            timeline.add(entry);
        }
        state.put("timeline", timeline);

        return state;
    }

    /**
     * Tạo JSON snapshot của order state tại thời điểm event.
     */
    private String buildSnapshot(Order order) {
        if (order == null) return null;
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", order.getId());
            snapshot.put("status", order.getStatus() != null ? order.getStatus().name() : null);
            snapshot.put("buyerId", order.getBuyerId());
            snapshot.put("sellerId", order.getSellerId());
            snapshot.put("totalPrice", order.getTotalPrice());
            snapshot.put("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("[EventStore] Failed to build snapshot: {}", e.getMessage());
            return null;
        }
    }
}
