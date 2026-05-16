package com.secondhand.authservice.service;

import com.secondhand.authservice.model.Addresses;

import java.util.List;

public interface AddressService {

    List<Addresses> getAddressesByUserId(String userId);

    Addresses getAddressById(Long id, String userId);

    Addresses getDefaultAddress(String userId);

    Addresses createAddress(String userId, Addresses address);

    Addresses updateAddress(Long id, String userId, Addresses address);

    void deleteAddress(Long id, String userId);

    Addresses setDefaultAddress(Long id, String userId);
}
