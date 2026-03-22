package com.secondhand.coreservice.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID Generator với format prefix-XXXX (vd: cg-0001, it-0001, etc)
 * Sử dụng timestamp + random để đảm bảo unique ngay cả sau khi restart
 */
public class IdGenerator {

    private static final Random random = new Random();
    private static final AtomicLong categoryCounter = new AtomicLong(0);
    private static final AtomicLong itemCounter = new AtomicLong(0);
    private static final AtomicLong attributeCounter = new AtomicLong(0);
    private static final AtomicLong attributeValueCounter = new AtomicLong(0);
    private static final AtomicLong itemImageCounter = new AtomicLong(0);
    private static final AtomicLong locationCounter = new AtomicLong(0);
    private static final AtomicLong reviewCounter = new AtomicLong(0);

    static {
        // Initialize counters dựa trên timestamp
        long timestamp = System.currentTimeMillis() % 10000;
        categoryCounter.set(timestamp);
        itemCounter.set(timestamp);
        attributeCounter.set(timestamp);
        attributeValueCounter.set(timestamp);
        itemImageCounter.set(timestamp);
        locationCounter.set(timestamp);
        reviewCounter.set(timestamp);
    }

    private static String generateId(String prefix, AtomicLong counter) {
        // Tăng counter và lấy giá trị
        long value = counter.incrementAndGet() % 10000;
        // Nếu ID trùng, tạo thêm random component
        if (value == 0) {
            value = 1 + random.nextInt(9999);
        }
        return String.format("%s-%04d", prefix, value);
    }

    public static String generateCategoryId() {
        return generateId("cg", categoryCounter);
    }

    public static String generateItemId() {
        return generateId("it", itemCounter);
    }

    public static String generateAttributeId() {
        return generateId("at", attributeCounter);
    }

    public static String generateAttributeValueId() {
        return generateId("av", attributeValueCounter);
    }

    public static String generateItemImageId() {
        return generateId("img", itemImageCounter);
    }

    public static String generateLocationId() {
        return generateId("loc", locationCounter);
    }

    public static String generateReviewId() {
        return generateId("rev", reviewCounter);
    }

    // Reset for testing (optional)
    public static void reset() {
        categoryCounter.set(0);
        itemCounter.set(0);
        attributeCounter.set(0);
        attributeValueCounter.set(0);
        itemImageCounter.set(0);
        locationCounter.set(0);
        reviewCounter.set(0);
    }
}
