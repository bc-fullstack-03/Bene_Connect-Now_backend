package com.sysmap.api.service.client.implementation;

import com.sysmap.api.domain.model.User;
import com.sysmap.api.domain.repo.UserRepository;
import com.sysmap.api.service.aws.FileStorageService;
import com.sysmap.api.service.client.IUservice;
import com.sysmap.api.service.client.dto.CreateUserRequest;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public final class UserService implements IUservice {

  @Autowired
  private UserRepository repo;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private FileStorageService fileStorageService;

  public String createUser(CreateUserRequest user) {
    var validateReggexEmail =
      user.getEmail().contains("@") && user.getEmail().contains(".com");
    var users = new User(user.getName(), user.getEmail(), user.getPassword());
    if (findByEmail(user.getEmail()) != null) {
      return ResponseEntity.ok("User already exists").toString();
    }
    var hash = passwordEncoder.encode(user.getPassword());
    users.setPassword(hash);
    if (!validateReggexEmail) {
      return ResponseEntity.ok("Invalid email").toString();
    }
    repo.save(users);
    return users.getId().toString();
  }

  public String updateUser(String id, CreateUserRequest request) {
    UUID uuid = UUID.fromString(id);
    var user = repo.findById(uuid);
    if (user.isEmpty()) {
      return ResponseEntity.ok("User not found").toString();
    }
    var users = user.get();
    users.setName(request.getName());
    repo.save(users);
    return users.toString();
  }

  public String findAll() {
    return repo.findAll().toString();
  }

  public User getUserById(UUID id) {
    return repo.findUserById(id).get();
  }

  public User findByEmail(String email) {
    var query = repo.findByEmail(email);
    if (query.isPresent()) {
      return query.get();
    }
    return null;
  }
  
  public User getUser(String email) {
    return repo.findByEmail(email).get();
}
  public boolean follow(String email) {
    var user = repo.findByEmail(email);
    if (user == null) {
      return false;
    }
    return repo.save(user.get()) != null;
  }

  public void uploadPhotoProfile(MultipartFile photo) throws Exception {
    var user =
      (
        (User) SecurityContextHolder
          .getContext()
          .getAuthentication()
          .getPrincipal()
      );

    var photoUri = "";

    try {
      var fileName =
        user.getId() +
        "." +
        photo
          .getOriginalFilename()
          .substring(photo.getOriginalFilename().lastIndexOf(".") + 1);

      photoUri = fileStorageService.upload(photo, fileName);
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }

    user.setPhotoUri(photoUri);
    repo.save(user);
  }
}
