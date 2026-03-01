package cl.duoc.finance_bff_atm.model;

import java.time.LocalDate;

/**
 * DTO que representa un movimiento/transaccion para la pantalla del cajero ATM.
 *
 * Contiene un formato reducido de la transaccion, optimizado para impresion
 * en voucher del cajero automatico (fecha, tipo de operacion y monto).
 *
 * @author Duoc UC - Backend 3
 */
public class MovimientoAtmDTO {

    /** Fecha en que se realizo la transaccion */
    private LocalDate fecha;

    /** Tipo de operacion realizada (ej: "Giro", "Abono", "Transferencia") */
    private String tipo;

    /** Monto de la transaccion en pesos */
    private Double monto;

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }
}
