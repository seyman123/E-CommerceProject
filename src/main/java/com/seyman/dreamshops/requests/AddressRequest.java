package com.seyman.dreamshops.requests;

import lombok.Data;

@Data
public class AddressRequest {
    private String title;
    private String fullName;
    private String address;
    private String city;
    private String district;
    private String postalCode;
    private String phone;
} 