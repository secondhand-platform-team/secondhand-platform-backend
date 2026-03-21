package com.secondhand.authservice.grpc;

import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.grpc.cart.CartServiceGrpc;
import com.secondhand.authservice.grpc.cart.CreateCartRequest;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CartGrpcClient {

    @GrpcClient("order-service")
    private CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub;

    public String createCart(String userId) {
        try {
            CreateCartRequest request = CreateCartRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return cartServiceBlockingStub.createCart(request).getCartId();
        } catch (StatusRuntimeException exception) {
            log.error("Create cart gRPC call failed for userId={}", userId, exception);
            throw new BadRequestException("Cannot create cart for user", exception);
        }
    }
}
