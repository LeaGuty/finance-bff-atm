package cl.duoc.finance_bff_atm.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class MovimientoAtmDTO {
    // Formato ultra-corto para imprimir en voucher
    private LocalDate fecha;
    private String tipo; // "Giro", "Abono", etc.
    private Double monto;
}