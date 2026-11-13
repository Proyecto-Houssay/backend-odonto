package com.proyectohoussay.odonto.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

@Embeddable
public class DisponibilidadHoraria {
    @NotNull
    @Enumerated(EnumType.STRING)
    private DiaSemana dia;
    @NotNull
    private LocalTime horaInicio;
    @NotNull
    private LocalTime horaFin;
    private Integer duracionPromedioMinutos;

    protected DisponibilidadHoraria() {}

    public DisponibilidadHoraria(DiaSemana dia, LocalTime horaInicio, LocalTime horaFin, Integer duracionPromedioMinutos) {
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionPromedioMinutos = duracionPromedioMinutos;
    }

    public DiaSemana getDia() { return dia; }
    public void setDia(DiaSemana dia) { this.dia = dia; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Integer getDuracionPromedioMinutos() { return duracionPromedioMinutos; }
    public void setDuracionPromedioMinutos(Integer duracionPromedioMinutos) { this.duracionPromedioMinutos = duracionPromedioMinutos; }
}
