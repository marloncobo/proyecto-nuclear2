package com.psicoapp.identity.application;

import java.util.UUID;

/** Convocations provide credential verification; participation must also check its own authorization. */
public interface StudentAccessVerifier {
    boolean verifies(UUID studentId, String code);
}
