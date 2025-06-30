package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.data.DataInitializer;
import com.seyman.dreamshops.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/data")
//@RequiredArgsConstructor
public class DataController {

    @Autowired(required = false)
    private DataInitializer dataInitializer;
    
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse> initializeData() {
        try {
            dataInitializer.onApplicationEvent(null);
            return ResponseEntity.ok(new ApiResponse("Data initialization completed successfully!", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse("Data initialization failed: " + e.getMessage(), null));
        }
    }
} 