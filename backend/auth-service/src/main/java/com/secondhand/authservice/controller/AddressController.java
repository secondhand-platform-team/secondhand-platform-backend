package com.secondhand.authservice.controller;

import com.secondhand.authservice.model.Addresses;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    private String getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return user.getUserId();
    }

    @GetMapping
    public ResponseEntity<List<Addresses>> getMyAddresses() {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(addressService.getAddressesByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Addresses> getAddress(@PathVariable Long id) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(addressService.getAddressById(id, userId));
    }

    @GetMapping("/default")
    public ResponseEntity<Addresses> getDefaultAddress() {
        String userId = getCurrentUserId();
        Addresses defaultAddr = addressService.getDefaultAddress(userId);
        if (defaultAddr == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(defaultAddr);
    }

    @PostMapping
    public ResponseEntity<Addresses> createAddress(@RequestBody Addresses address) {
        String userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.createAddress(userId, address));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Addresses> updateAddress(
            @PathVariable Long id,
            @RequestBody Addresses address) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(addressService.updateAddress(id, userId, address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAddress(@PathVariable Long id) {
        String userId = getCurrentUserId();
        addressService.deleteAddress(id, userId);
        return ResponseEntity.ok(Map.of("message", "Xóa địa chỉ thành công"));
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<Addresses> setDefaultAddress(@PathVariable Long id) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(addressService.setDefaultAddress(id, userId));
    }
}
