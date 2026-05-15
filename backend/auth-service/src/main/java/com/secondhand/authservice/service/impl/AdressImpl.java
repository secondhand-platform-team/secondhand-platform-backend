package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.model.Addresses;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.AddressRepository;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdressImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public List<Addresses> getAddressesByUserId(String userId) {
        return addressRepository.findByUserUserId(userId);
    }

    @Override
    public Addresses getAddressById(Long id, String userId) {
        return addressRepository.findByIdAndUserUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
    }

    @Override
    public Addresses getDefaultAddress(String userId) {
        return addressRepository.findByUserUserIdAndMain(userId, 1).orElse(null);
    }

    @Override
    @Transactional
    public Addresses createAddress(String userId, Addresses address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        address.setUser(user);

        // If this is the first address or marked as default, set as main
        List<Addresses> existingAddresses = addressRepository.findByUserUserId(userId);
        if (existingAddresses.isEmpty()) {
            address.setMain(1); // first address is always default
        } else if (address.getMain() == 1) {
            // Remove default from other addresses
            existingAddresses.forEach(a -> {
                if (a.getMain() == 1) {
                    a.setMain(0);
                    addressRepository.save(a);
                }
            });
        }

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Addresses updateAddress(Long id, String userId, Addresses updatedAddress) {
        Addresses existing = addressRepository.findByIdAndUserUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        existing.setReceiverName(updatedAddress.getReceiverName());
        existing.setReceiverPhone(updatedAddress.getReceiverPhone());
        existing.setProvince(updatedAddress.getProvince());
        existing.setDistrict(updatedAddress.getDistrict());
        existing.setWard(updatedAddress.getWard());
        existing.setSpecifics(updatedAddress.getSpecifics());

        if (updatedAddress.getMain() == 1 && existing.getMain() != 1) {
            // Set this as default, remove default from others
            List<Addresses> allAddresses = addressRepository.findByUserUserId(userId);
            allAddresses.forEach(a -> {
                if (a.getMain() == 1 && !a.getId().equals(id)) {
                    a.setMain(0);
                    addressRepository.save(a);
                }
            });
            existing.setMain(1);
        }

        return addressRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id, String userId) {
        Addresses address = addressRepository.findByIdAndUserUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        boolean wasDefault = address.getMain() == 1;
        addressRepository.delete(address);

        // If deleted address was default, set the first remaining address as default
        if (wasDefault) {
            List<Addresses> remaining = addressRepository.findByUserUserId(userId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setMain(1);
                addressRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public Addresses setDefaultAddress(Long id, String userId) {
        Addresses address = addressRepository.findByIdAndUserUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Remove default from all other addresses
        List<Addresses> allAddresses = addressRepository.findByUserUserId(userId);
        allAddresses.forEach(a -> {
            if (a.getMain() == 1) {
                a.setMain(0);
                addressRepository.save(a);
            }
        });

        // Set new default
        address.setMain(1);
        return addressRepository.save(address);
    }
}
