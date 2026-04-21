package com.secondhand.coreservice.utils;

import java.util.UUID;

/**
 * ID Generator sử dụng UUID để đảm bảo unique globally
 */
public class IdGenerator {

    public static String generateCategoryId() {
        return UUID.randomUUID().toString();
    }

    public static String generateItemId() {
        return UUID.randomUUID().toString();
    }

    public static String generateAttributeId() {
        return UUID.randomUUID().toString();
    }

    public static String generateAttributeValueId() {
        return UUID.randomUUID().toString();
    }

    public static String generateItemImageId() {
        return UUID.randomUUID().toString();
    }

    public static String generateLocationId() {
        return UUID.randomUUID().toString();
    }

    public static String generateReviewId() {
        return UUID.randomUUID().toString();
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
