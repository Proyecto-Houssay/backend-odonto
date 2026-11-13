package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.PersonalRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DirectorioServiceTest {
    private final PersonalRepository personalRepository = mock(PersonalRepository.class);
    private final OdontologoRepository odontologoRepository = mock(OdontologoRepository.class);
    private final PacienteRepository pacienteRepository = mock(PacienteRepository.class);
    private final DirectorioService service = new DirectorioService(personalRepository, odontologoRepository, pacienteRepository);

    @Test
    void searchesAcrossPeopleAndFiltersByType() {
        Personal p = new Personal("Carla", "López", "1234", "carla@clinic.test", "111", CargoPersonal.RECEPCIONISTA,
                LocalDate.now(), "Mañana", null);
        Paciente patient = new Paciente();
        patient.setNombre("Carla");
        patient.setApellido("Paz");
        patient.setDni("9999");
        Especialidad esp = new Especialidad("Ortodoncia", "");
        Odontologo doctor = new Odontologo("Carla", "Méndez", "MN-100", "doctor@clinic.test", "222", esp);
        when(personalRepository.findAll()).thenReturn(List.of(p));
        when(odontologoRepository.findAll()).thenReturn(List.of(doctor));
        when(pacienteRepository.findAll()).thenReturn(List.of(patient));

        assertThat(service.buscar("carla", null)).hasSize(3);
        assertThat(service.buscar("carla", "PACIENTE")).extracting("tipo").containsExactly("PACIENTE");
    }

    @Test
    void rejectsUnsupportedPersonType() {
        assertThatThrownBy(() -> service.buscar("ana", "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PERSONAL, MEDICO o PACIENTE");
    }
}
