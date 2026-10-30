package com.psicoapp.identity.application;

import java.util.UUID;

/** Public contract for other modules; never exposes credentials or internal entities. */
public interface IdentityDirectory {
    record Person(UUID id, String email, String name, String role, boolean canCreateCases) { }

    Person requireTeacher(UUID userId);
    Person enrollStudent(UUID teacherId, String email, String name);
}
