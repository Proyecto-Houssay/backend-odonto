CREATE TABLE pacientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    documento VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE odontologos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    matricula VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE turnos (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    odontologo_id BIGINT NOT NULL REFERENCES odontologos(id),
    fecha DATE NOT NULL,
    horario TIME NOT NULL,
    motivo VARCHAR(500),
    estado VARCHAR(20) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_turnos_odontologo_fecha_horario
    ON turnos (odontologo_id, fecha, horario);