package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.department.model.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h1>DepartmentRepositoryPort</h1>
 * <p>
 * Output Port interface representing persistence capabilities for the {@link Department} domain entity.
 * Implemented by persistence adapters in the infrastructure layer.
 * </p>
 */
public interface DepartmentRepositoryPort {

    /**
     * Persists or updates a department.
     *
     * @param department the department to save
     * @return the saved department
     */
    Department save(Department department);

    /**
     * Resolves a department by ID.
     *
     * @param id the department UUID
     * @return an {@link Optional} containing the department, or empty
     */
    Optional<Department> findById(UUID id);

    /**
     * Lists all departments.
     *
     * @return list of all departments
     */
    List<Department> findAll();

    /**
     * Deletes a department by ID.
     *
     * @param id the department UUID
     */
    void delete(UUID id);
}
