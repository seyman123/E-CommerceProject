package com.seyman.dreamshops.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String fullName;
    private String address;
    private String city;
    private String district;
    private String postalCode;
    private String phone;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
} 