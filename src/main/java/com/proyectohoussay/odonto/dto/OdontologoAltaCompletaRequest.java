package com.proyectohoussay.odonto.dto;

public record OdontologoAltaCompletaRequest(
        String nombre,
        String apellido,
        String matricula,
        String email,
        String telefono,
        String consultorio,
        Boolean activo,
        Long especialidadId,
        String nuevaEspecialidadNombre,
        String nuevaEspecialidadDescripcion,
        Boolean crearCuentaUsuario,
        String username,
        String password
) {
}
