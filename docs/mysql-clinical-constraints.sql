-- Migración MySQL 8 de una sola vez para una base académica existente.
-- En Workbench, seleccione primero el esquema correcto. Ejecute los SELECT
-- por separado; NO ejecute todo el archivo de una vez. Haga una copia de
-- seguridad antes de ejecutar el ALTER TABLE del final.

-- 1. Confirme el esquema. Deténgase si es NULL o una base incorrecta.
SELECT DATABASE() AS selected_schema;

-- 2. Ambas consultas deben devolver cero filas. No borre ni modifique datos
-- para forzar las restricciones; revise cualquier fila con el equipo.
SELECT paciente_id, COUNT(*) AS history_count
FROM historias_clinicas
GROUP BY paciente_id
HAVING COUNT(*) > 1;

SELECT h.id AS history_id, h.paciente_id
FROM historias_clinicas h
LEFT JOIN paciente p ON p.id = h.paciente_id
WHERE p.id IS NULL;

-- 3. Compruebe que no existan ya restricciones equivalentes UNIQUE y FOREIGN KEY.
-- Si existen, omita o ajuste el DDL; no cree restricciones duplicadas.
SELECT tc.constraint_name, tc.constraint_type, kcu.column_name,
       kcu.referenced_table_name, kcu.referenced_column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON kcu.constraint_schema = tc.constraint_schema
 AND kcu.table_name = tc.table_name
 AND kcu.constraint_name = tc.constraint_name
WHERE tc.table_schema = DATABASE()
  AND tc.table_name = 'historias_clinicas'
  AND tc.constraint_type IN ('UNIQUE', 'FOREIGN KEY');

-- 4. Ejecute SOLO este ALTER TABLE después de comprobar y respaldar los datos.
-- MySQL confirma DDL implícitamente. Este comando no elimina filas existentes.
-- ON DELETE RESTRICT impide borrar por SQL un paciente con historia clínica.
ALTER TABLE historias_clinicas
  ADD CONSTRAINT uq_historias_clinicas_paciente UNIQUE (paciente_id),
  ADD CONSTRAINT fk_historias_clinicas_paciente
    FOREIGN KEY (paciente_id) REFERENCES paciente (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

-- 5. Repita el paso 3 para comprobar que aparezcan ambas restricciones.
