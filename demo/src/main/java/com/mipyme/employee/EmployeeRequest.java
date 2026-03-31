package com.mipyme.employee;

import java.util.List;

public record EmployeeRequest(
    String name,
    String email,
    String password,
    List<String> permissions,
    List<Long> warehouseIds
) {}
