package com.seyman.dreamshops.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class TestDbController {
    @Autowired
    private DataSource dataSource;

    @GetMapping("/test-db")
    public String testDb() {
        try (Connection conn = dataSource.getConnection()) {
            return "✅ Connected to DB: " + conn.getMetaData().getURL();
        } catch (Exception e) {
            return "❌ DB connection failed: " + e.getMessage();
        }
    }
}
