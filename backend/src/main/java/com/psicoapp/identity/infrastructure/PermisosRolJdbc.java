package com.psicoapp.identity.infrastructure;

import com.psicoapp.identity.application.PermisosRol;
import com.psicoapp.identity.domain.Rol;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PermisosRolJdbc implements PermisosRol {
    private final JdbcClient jdbc;

    public PermisosRolJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean puedeCrearCasos(Rol rol) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM rol_permiso rp
                JOIN rol r ON r.id_rol = rp.id_rol JOIN permiso p ON p.id_permiso = rp.id_permiso
                WHERE r.nombre = ? AND p.nombre = 'CREAR_CASOS'
                """).param(rol.name()).query(Integer.class).single() > 0;
    }

    @Override
    public void configurarCreacionCasos(Rol rol, boolean habilitado) {
        if (habilitado) {
            jdbc.sql("""
                    INSERT INTO rol_permiso (id_rol, id_permiso)
                    SELECT r.id_rol, p.id_permiso FROM rol r CROSS JOIN permiso p
                    WHERE r.nombre = ? AND p.nombre = 'CREAR_CASOS'
                    ON DUPLICATE KEY UPDATE id_permiso = rol_permiso.id_permiso
                    """).param(rol.name()).update();
        } else {
            jdbc.sql("""
                    DELETE rp FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol
                    JOIN permiso p ON p.id_permiso = rp.id_permiso
                    WHERE r.nombre = ? AND p.nombre = 'CREAR_CASOS'
                    """).param(rol.name()).update();
        }
    }
}
