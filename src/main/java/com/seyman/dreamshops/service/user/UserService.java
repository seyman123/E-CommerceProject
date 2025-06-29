package com.seyman.dreamshops.service.user;

import com.seyman.dreamshops.dto.UserDto;
import com.seyman.dreamshops.exceptions.AlreadyExistsException;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.User;
import com.seyman.dreamshops.repository.UserRepository;
import com.seyman.dreamshops.requests.CreateUserRequest;
import com.seyman.dreamshops.requests.PasswordChangeRequest;
import com.seyman.dreamshops.requests.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = "users", key = "#userId")
    public UserDto getUserById(Long userId) {
        return userRepository.findById(userId).map(this::convertUserToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
    }

    @Override
    @CacheEvict(value = "users", key = "#request.email")
    public UserDto createUser(CreateUserRequest request) {
        return Optional.of(request).filter(user -> !userRepository.existsByEmail(request.getEmail())).map(req -> {
            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            return userRepository.save(user);
        }).map(this::convertUserToDto)
                .orElseThrow(() -> new AlreadyExistsException("Oops! " + request.getEmail() + " already exists!"));
    }

    @Override
    @CachePut(value = "users", key = "#userId")
    public UserDto updateUser(UserUpdateRequest request, Long userId) {
        return userRepository.findById(userId).map(existingUser -> {
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            existingUser.setPhone(request.getPhone());
            existingUser.setDateOfBirth(request.getDateOfBirth());
            if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new AlreadyExistsException("Email already exists: " + request.getEmail());
                }
                existingUser.setEmail(request.getEmail());
            }
            return userRepository.save(existingUser);
        }).map(this::convertUserToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
    }

    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.findById(userId).ifPresentOrElse(userRepository :: delete, () -> {
            throw new ResourceNotFoundException("User not found!");
        });
    }

    @Override
    @CachePut(value = "users", key = "#userId")
    public void changePassword(PasswordChangeRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public UserDto convertUserToDto(User user) {
        if (user == null) {
            return null;
        }
        
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setDateOfBirth(user.getDateOfBirth());
        
        return userDto;
    }

    @Override
    public User convertDtoToUser(UserDto userDto) {
        if (userDto == null) {
            return null;
        }
        
        User user = new User();
        user.setId(userDto.getId());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setDateOfBirth(userDto.getDateOfBirth());
        
        return user;
    }

    @Override
    @Cacheable(value = "users", key = "#email")
    public UserDto getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        
        String email = authentication.getName();
        if (email == null || email.isEmpty()) {
            throw new ResourceNotFoundException("User email not found in authentication");
        }
        
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        
        return this.convertUserToDto(user);
    }
}
