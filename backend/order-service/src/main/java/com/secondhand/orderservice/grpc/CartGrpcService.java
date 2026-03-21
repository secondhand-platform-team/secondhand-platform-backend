package com.secondhand.orderservice.grpc;

import com.secondhand.orderservice.grpc.cart.CartServiceGrpc;
import com.secondhand.orderservice.grpc.cart.CreateCartRequest;
import com.secondhand.orderservice.grpc.cart.CreateCartResponse;
import com.secondhand.orderservice.model.Cart;
import com.secondhand.orderservice.service.CartService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CartGrpcService extends CartServiceGrpc.CartServiceImplBase {

    private final CartService cartService;

    @Override
    public void createCart(CreateCartRequest request, StreamObserver<CreateCartResponse> responseObserver) {
        try {
            String userId = request.getUserId();
            if (userId == null || userId.isBlank()) {
                throw Status.INVALID_ARGUMENT.withDescription("userId is required").asRuntimeException();
            }

            Cart cart = cartService.createOrGetCart(userId);
            CreateCartResponse response = CreateCartResponse.newBuilder()
                    .setCartId(cart.getId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(exception);
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unable to create cart")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
