package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.model.Address;
import com.seyman.dreamshops.requests.AddressRequest;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.address.IAddressService;
import com.seyman.dreamshops.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("${api.prefix}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final IAddressService addressService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<ApiResponse> addAddress(@RequestBody AddressRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            Address address = addressService.addAddress(request, userId);
            return ResponseEntity.ok(new ApiResponse("Adres başarıyla eklendi", address));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Adres eklenirken hata oluştu: " + e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getUserAddresses(HttpServletRequest httpRequest) {
        try {
            Long userId = getUserIdFromToken(httpRequest);
            List<Address> addresses = addressService.getUserAddresses(userId);
            return ResponseEntity.ok(new ApiResponse("Adresler getirildi", addresses));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Adresler getirilirken hata oluştu: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse> getAddressById(@PathVariable Long addressId) {
        try {
            Address address = addressService.getAddressById(addressId);
            return ResponseEntity.ok(new ApiResponse("Adres bulundu", address));
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiResponse("Adres bulunamadı: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse> updateAddress(@PathVariable Long addressId, @RequestBody AddressRequest request) {
        try {
            Address updatedAddress = addressService.updateAddress(addressId, request);
            return ResponseEntity.ok(new ApiResponse("Adres güncellendi", updatedAddress));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Adres güncellenirken hata oluştu: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse> deleteAddress(@PathVariable Long addressId) {
        try {
            addressService.deleteAddress(addressId);
            return ResponseEntity.ok(new ApiResponse("Adres silindi", null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Adres silinirken hata oluştu: " + e.getMessage(), null));
        }
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = jwtUtils.getTokenFromRequest(request);
        return jwtUtils.getUserIdFromToken(token);
    }
} 