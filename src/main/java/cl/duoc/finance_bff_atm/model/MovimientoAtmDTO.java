package cl.duoc.finance_bff_atm.model;

import java.time.LocalDate;

import lombok.Data;

/**
 * DTO que representa un movimiento/transaccion para la pantalla del cajero ATM.
 *
 * Contiene un formato reducido de la transaccion, optimizado para impresion
 * en voucher del cajero automatico (fecha, tipo de operacion y monto).
 *
 * Se utiliza Lombok @Data para generar automaticamente getters, setters,
 * toString, equals y hashCode.
 *
 * @author Duoc UC - Backend 3
 */
@Data
public class MovimientoAtmDTO {

    /** Fecha en que se realizo la transaccion */
    private LocalDate fecha;

    /** Tipo de operacion realizada (ej: "Giro", "Abono", "Transferencia") */
    private String tipo;

    /** Monto de la transaccion en pesos */
    private Double monto;
}
