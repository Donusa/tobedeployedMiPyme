package com.mipyme.employee;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.mipyme.company.PlanLimitException;
import com.mipyme.company.PlanLimitService;
import com.mipyme.company.ResourceType;
import com.mipyme.tenant.TenantContext;
import com.mipyme.user.AppUser;
import com.mipyme.user.AppUserRepository;
import com.mipyme.user.UserRole;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlanLimitService planLimitService;

    public EmployeeController(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            PlanLimitService planLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.planLimitService = planLimitService;
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return userRepository.findAll().stream()
                .filter(u -> UserRole.EMPLOYEE.name().equals(u.getRole()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeRequest request) {
        if (userRepository.existsByUsername(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String tenantId = TenantContext.getCurrentTenant();
        try {
            planLimitService.assertResourceLimit(tenantId, ResourceType.USERS);
        } catch (PlanLimitException ex) {
            return ResponseEntity.status(422)
                    .body(java.util.Map.of("message", ex.getMessage()));
        }

        String permissions = request.permissions() != null
            ? String.join(",", request.permissions())
            : "";

        String warehouseIds = request.warehouseIds() != null
            ? request.warehouseIds().stream().map(String::valueOf).collect(Collectors.joining(","))
            : "";

        AppUser user = new AppUser(
            request.email(),
            request.name(),
            request.email(),
            passwordEncoder.encode(request.password()),
            UserRole.EMPLOYEE.name(),
            permissions
        );
        user.setWarehouseIds(warehouseIds);

        AppUser saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(request.name());
                    user.setEmail(request.email());
                    user.setUsername(request.email());

                    if (request.password() != null && !request.password().isBlank()) {
                        user.setPasswordHash(passwordEncoder.encode(request.password()));
                    }

                    if (request.permissions() != null) {
                         user.setPermissions(String.join(",", request.permissions()));
                    }

                    if (request.warehouseIds() != null) {
                        user.setWarehouseIds(request.warehouseIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
                    }

                    return ResponseEntity.ok(toResponse(userRepository.save(user)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private EmployeeResponse toResponse(AppUser user) {
        List<String> perms = user.getPermissions() != null && !user.getPermissions().isBlank()
                ? Arrays.asList(user.getPermissions().split(","))
                : List.of();
        List<Long> wIds = user.getWarehouseIds() != null && !user.getWarehouseIds().isBlank()
                ? Arrays.stream(user.getWarehouseIds().split(",")).map(Long::valueOf).collect(Collectors.toList())
                : List.of();
        return new EmployeeResponse(user.getId(), user.getName(), user.getEmail(), perms, wIds);
    }
}
