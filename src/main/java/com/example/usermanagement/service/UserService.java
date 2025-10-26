package com.example.usermanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.usermanagement.dto.UserDTOs;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.CustomExceptions;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.security.CustomUserDetails;
import com.example.usermanagement.security.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ImageKitService imageKitService;

    @Transactional
    public UserDTOs.AuthResponse signup(UserDTOs.SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomExceptions.DuplicateResourceException("Email already exists");
        }

        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new CustomExceptions.DuplicateResourceException("Username already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtUtil.generateToken(userDetails);

        return new UserDTOs.AuthResponse(token, convertToUserResponse(savedUser));
    }

    public UserDTOs.AuthResponse login(UserDTOs.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            return new UserDTOs.AuthResponse(token, convertToUserResponse(userDetails.getUser()));
        } catch (Exception e) {
            throw new CustomExceptions.InvalidCredentialsException("Invalid email or password");
        }
    }

    public List<UserDTOs.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public UserDTOs.UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("User not found with id: " + id));
        return convertToUserResponse(user);
    }

    @Transactional
    public UserDTOs.UserResponse updateUser(Long id, UserDTOs.UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("User not found with id: " + id));

        validateUserAccess(user);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getUsername() != null) {
            if (!request.getUsername().equals(user.getUsername()) &&
                    userRepository.existsByUsername(request.getUsername())) {
                throw new CustomExceptions.DuplicateResourceException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public UserDTOs.UserResponse uploadProfilePicture(Long id, MultipartFile file) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("User not found with id: " + id));

        validateUserAccess(user);

        if (file.isEmpty()) {
            throw new CustomExceptions.FileUploadException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomExceptions.FileUploadException("Only image files are allowed");
        }

        if (user.getProfilePictureFileId() != null) {
            try {
                imageKitService.deleteImage(user.getProfilePictureFileId());
            } catch (Exception e) {
                
            }
        }

        Map<String, String> uploadResult = imageKitService.uploadImage(file, "user-profiles");

        user.setProfilePictureUrl(uploadResult.get("url"));
        user.setProfilePictureFileId(uploadResult.get("fileId"));

        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("User not found with id: " + id));

        validateUserAccess(user);

        if (user.getProfilePictureFileId() != null) {
            try {
                imageKitService.deleteImage(user.getProfilePictureFileId());
            } catch (Exception e) {
                
            }
        }

        userRepository.delete(user);
    }

    private void validateUserAccess(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        if (!currentUser.getUser().getId().equals(user.getId())) {
            throw new CustomExceptions.UnauthorizedException("You are not authorized to perform this action");
        }
    }

    private UserDTOs.UserResponse convertToUserResponse(User user) {
        UserDTOs.UserResponse response = new UserDTOs.UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setUsername(user.getUsername());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}