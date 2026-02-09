package cl.duoc.finance_bff_atm.model;

import java.util.List;

import lombok.Data;

/**
 * DTO que representa el resumen de cuenta mostrado en la pantalla del cajero ATM.
 *
 * Contiene toda la informacion necesaria para la interfaz del cajero automatico:
 * mensaje del sistema, nombre enmascarado del cliente (por seguridad),
 * saldo actual y los ultimos movimientos para impresion en voucher.
 *
 * Se utiliza Lombok @Data para generar automaticamente getters, setters,
 * toString, equals y hashCode.
 *
 * @author Duoc UC - Backend 3
 */
@Data
public class ResumenCajeroDTO {

    /** Mensaje del sistema para mostrar en pantalla (ej: "Operacion Exitosa", mensajes de error) */
    private String mensajeSistema;

    /** Nombre del cliente enmascarado por seguridad ATM (ej: "J*** P****") */
    private String nombreClienteEnmascarado;

    /** Saldo actual de la cuenta en pesos */
    private Double saldoActual;

    /** Ultimos 3 movimientos de la cuenta, ordenados por fecha descendente (para voucher) */
    private List<MovimientoAtmDTO> ultimos3Movimientos;
}
