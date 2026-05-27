package com.secondhand.orderservice.service;

/**
 * CQRS — Combined Service Interface (backward compatible)
 * 
 * Extends cả OrderCommandService (Write) và OrderQueryService (Read).
 * Controller inject interface này, nhưng bên trong implementation
 * được tách thành 2 class riêng biệt:
 * - OrderCommandServiceImpl: xử lý write
 * - OrderQueryServiceImpl: xử lý read
 * 
 * Đây là CQRS Pattern ở mức Application Layer.
 */
public interface OrderService extends OrderCommandService, OrderQueryService {
    // Không cần khai báo thêm method — kế thừa từ 2 interface cha
}
