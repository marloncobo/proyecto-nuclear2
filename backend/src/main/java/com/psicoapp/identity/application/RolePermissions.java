package com.psicoapp.identity.application;

import com.psicoapp.identity.domain.Role;

/** The ER assigns permissions to roles, not individual users. */
public interface RolePermissions {
    boolean canCreateCases(Role role);
    void setCaseCreation(Role role, boolean enabled);
}
