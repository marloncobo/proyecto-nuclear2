package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.RolePermissions;
import com.psicoapp.identity.domain.Role;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRolePermissions implements RolePermissions {
    private final JdbcClient jdbc;

    public JdbcRolePermissions(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean canCreateCases(Role role) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM rol_permiso rp
                JOIN rol r ON r.id_rol = rp.id_rol JOIN permiso p ON p.id_permiso = rp.id_permiso
                WHERE r.nombre = ? AND p.nombre = 'CREAR_CASOS'
                """).param(role.name()).query(Integer.class).single() > 0;
    }

    @Override
    public void setCaseCreation(Role role, boolean enabled) {
        if (enabled) {
            jdbc.sql("""
                    INSERT INTO rol_permiso (id_rol, id_permiso)
                    SELECT r.id_rol, p.id_permiso FROM rol r CROSS JOIN permiso p
                    WHERE r.nombre = ? AND p.nombre = 'CREAR_CASOS'
                    ON DUPLICATE KEY UPDATE id_permiso = rol_permiso.id_permiso
                    """).param(role.name()).update();
        } else {
            jdbc.sql("""
                    DELETE rp FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol
                    JOIN permiso p ON p.id_permiso = rp.id_permiso
                    WHERE r.nombre = ? AND p.nombre = 'CREAR_CASOS'
                    """).param(role.name()).update();
        }
    }
}