package com.secondhand.authservice.service;

public interface UserService {
    boolean decreaseFreeSellUse(String userId);

    int getFreeSellUsed(String userId);
}
