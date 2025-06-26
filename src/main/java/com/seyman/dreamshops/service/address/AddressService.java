package com.seyman.dreamshops.service.address;

import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Address;
import com.seyman.dreamshops.model.User;
import com.seyman.dreamshops.repository.AddressRepository;
import com.seyman.dreamshops.repository.UserRepository;
import com.seyman.dreamshops.requests.AddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService implements IAddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public Address addAddress(AddressRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        Address address = new Address();
        address.setTitle(request.getTitle());
        address.setFullName(request.getFullName());
        address.setAddress(request.getAddress());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setPostalCode(request.getPostalCode());
        address.setPhone(request.getPhone());
        address.setUser(user);

        return addressRepository.save(address);
    }

    @Override
    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    @Override
    public Address getAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı"));
    }

    @Override
    public void deleteAddress(Long addressId) {
        Address address = getAddressById(addressId);
        addressRepository.delete(address);
    }

    @Override
    public Address updateAddress(Long addressId, AddressRequest request) {
        Address address = getAddressById(addressId);
        
        address.setTitle(request.getTitle());
        address.setFullName(request.getFullName());
        address.setAddress(request.getAddress());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setPostalCode(request.getPostalCode());
        address.setPhone(request.getPhone());

        return addressRepository.save(address);
    }
} 