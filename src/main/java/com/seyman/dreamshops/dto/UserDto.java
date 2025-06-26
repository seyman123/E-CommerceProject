package com.seyman.dreamshops.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String dateOfBirth;
    private List<OrderDto> orders;
    private CartDto cart;
}
