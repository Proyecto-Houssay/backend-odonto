package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.dto.MovimientoFinancieroDto;
import com.proyectohoussay.odonto.dto.MovimientoFinancieroRequest;
import com.proyectohoussay.odonto.model.MovimientoFinanciero;
import com.proyectohoussay.odonto.model.TipoMovimientoFinanciero;
import com.proyectohoussay.odonto.repository.MovimientoFinancieroRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class MovimientoFinancieroService {

    private final MovimientoFinancieroRepository movimientoRepository;
    private final TurnoRepository turnoRepository;

    public MovimientoFinancieroService(MovimientoFinancieroRepository movimientoRepository,
                                       TurnoRepository turnoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.turnoRepository = turnoRepository;
    }

    @Transactional
    public MovimientoFinancieroDto registrar(MovimientoFinancieroRequest request) {
        if (request.tipo() == TipoMovimientoFinanciero.INGRESO && request.turnoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un ingreso debe vincularse a un turno registrado");
        }
        if (request.tipo() == TipoMovimientoFinanciero.EGRESO && request.turnoId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un egreso no puede vincularse a un turno");
        }

        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setTipo(request.tipo());
        movimiento.setFecha(request.fecha());
        movimiento.setImporte(request.importe());
        movimiento.setConcepto(request.concepto().trim());
        movimiento.setMedioPago(request.medioPago() == null ? null : request.medioPago().trim());
        if (request.turnoId() != null) {
            movimiento.setTurno(turnoRepository.findById(request.turnoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado")));
        }
        return toDto(movimientoRepository.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<MovimientoFinancieroDto> cobros(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return movimientoRepository.findByFechaBetweenAndTipo(desde, hasta, TipoMovimientoFinanciero.INGRESO)
                .stream().map(this::toDto).toList();
    }

    public static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar un período completo y desde no puede ser posterior a hasta");
        }
    }

    private MovimientoFinancieroDto toDto(MovimientoFinanciero movimiento) {
        return new MovimientoFinancieroDto(movimiento.getId(), movimiento.getFecha(), movimiento.getTipo(),
                movimiento.getImporte(), movimiento.getConcepto(), movimiento.getMedioPago(),
                movimiento.getTurno() == null ? null : movimiento.getTurno().getId());
    }
}
