package com.seyman.dreamshops.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.annotations.NaturalId;

@Data
public class CreateUserRequest {
    
    @NotBlank(message = "Ad gereklidir")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ\\s]+$", message = "Ad sadece harflerden oluşmalıdır")
    private String firstName;
    
    @NotBlank(message = "Soyad gereklidir")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    @Pattern(regexp = "^[a-zA-ZçÇğĞıİöÖşŞüÜ\\s]+$", message = "Soyad sadece harflerden oluşmalıdır")
    private String lastName;
    
    @NotBlank(message = "E-posta adresi gereklidir")
    @Email(message = "Geçerli bir e-posta adresi girin")
    @Size(max = 100, message = "E-posta adresi çok uzun")
    private String email;
    
    @NotBlank(message = "Şifre gereklidir")
    @Size(min = 6, max = 100, message = "Şifre 6-100 karakter arasında olmalıdır")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", 
             message = "Şifre en az bir küçük harf, bir büyük harf ve bir rakam içermelidir")
    private String password;
}
