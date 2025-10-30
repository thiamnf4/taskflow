package com.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private Long tenantId;
    private String tenantName;
    
    public AuthResponse(String token, Long userId, String email, String firstName, 
                       String lastName, Long tenantId, String tenantName) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
    }
}
