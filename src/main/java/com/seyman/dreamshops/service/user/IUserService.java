package com.seyman.dreamshops.service.user;

import com.seyman.dreamshops.dto.UserDto;
import com.seyman.dreamshops.model.User;
import com.seyman.dreamshops.requests.CreateUserRequest;
import com.seyman.dreamshops.requests.PasswordChangeRequest;
import com.seyman.dreamshops.requests.UserUpdateRequest;

public interface IUserService {

    UserDto getUserById(Long userId);
    UserDto createUser(CreateUserRequest request);
    UserDto updateUser(UserUpdateRequest request, Long userId);
    void deleteUser(Long userId);
    void changePassword(PasswordChangeRequest request, Long userId);

    UserDto convertUserToDto(User user);

    User convertDtoToUser(UserDto userDto);

    UserDto getAuthenticatedUser();
}
