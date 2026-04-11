package com.secondhand.authservice.grpc;

import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.grpc.cart.CartServiceGrpc;
import com.secondhand.authservice.grpc.cart.CreateCartRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CartGrpcClient {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 500;

    @GrpcClient("order-service")
    private CartServiceGrpc.CartServiceBlockingStub cartServiceBlockingStub;

    public String createCart(String userId) {
        CreateCartRequest request = CreateCartRequest.newBuilder()
                .setUserId(userId)
                .build();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return cartServiceBlockingStub
                        .withWaitForReady()
                        .withDeadlineAfter(3, TimeUnit.SECONDS)
                        .createCart(request)
                        .getCartId();
            } catch (StatusRuntimeException exception) {
                Status.Code code = exception.getStatus().getCode();
                boolean isTransient = code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED;
                boolean hasMoreAttempts = attempt < MAX_RETRIES;

                if (!isTransient || !hasMoreAttempts) {
                    log.error("Create cart gRPC call failed for userId={} after {} attempt(s)", userId, attempt, exception);
                    throw new BadRequestException("Cannot create cart for user", exception);
                }

                long delay = BASE_RETRY_DELAY_MS * attempt;
                log.warn("Create cart gRPC call transient failure for userId={} on attempt {}. Retrying in {} ms", userId, attempt, delay);
                sleepBeforeRetry(delay);
            }
        }

        throw new BadRequestException("Cannot create cart for user");
    }

    private void sleepBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Cart creation interrupted", interruptedException);
        }
    }
}
