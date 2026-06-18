package com.hotelmanager.web;

import com.hotelmanager.service.UserService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.UserCreateRequest;
import com.hotelmanager.web.dto.UserDto;
import com.hotelmanager.web.dto.UserStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<UserDto>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(userService.list(pageable)));
    }

    @Operation(summary = "Create user")
    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest req) {
        UserDto created = userService.create(req);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserCreateRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    @Operation(summary = "Activate/deactivate user")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> updateStatus(@PathVariable Long id, @RequestBody UserStatusRequest req) {
        return ResponseEntity.ok(userService.updateStatus(id, req));
    }
}
