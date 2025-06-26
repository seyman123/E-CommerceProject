package com.seyman.dreamshops.service.address;

import com.seyman.dreamshops.model.Address;
import com.seyman.dreamshops.requests.AddressRequest;

import java.util.List;

public interface IAddressService {
    Address addAddress(AddressRequest request, Long userId);
    List<Address> getUserAddresses(Long userId);
    Address getAddressById(Long addressId);
    void deleteAddress(Long addressId);
    Address updateAddress(Long addressId, AddressRequest request);
} 