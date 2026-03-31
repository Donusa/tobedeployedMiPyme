package com.mipyme.employee;

import java.util.List;

public record EmployeeResponse(
    Long id,
    String name,
    String email,
    List<String> permissions,
    List<Long> warehouseIds
) {}
