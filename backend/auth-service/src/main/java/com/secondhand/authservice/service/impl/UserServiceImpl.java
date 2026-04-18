package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public boolean decreaseFreeSellUse(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        int freeSellUse = user.getFreeSellUsed();

        if (freeSellUse <= 0) {
            throw new RuntimeException("No free sell uses remaining");
        }
        user.setFreeSellUsed(freeSellUse - 1);
        userRepository.save(user);

        return true;
    }

    @Override
    public int getFreeSellUsed(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFreeSellUsed();
    }

}
