package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;

}
