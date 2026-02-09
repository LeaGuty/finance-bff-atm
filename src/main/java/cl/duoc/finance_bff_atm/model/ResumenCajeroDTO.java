package cl.duoc.finance_bff_atm.model;

import java.util.List;

import lombok.Data;

@Data
public class ResumenCajeroDTO {
    private String mensajeSistema; // Ej: "Operación Exitosa"
    private String nombreClienteEnmascarado; // SEGURIDAD: Nombre oculto
    private Double saldoActual;
    private List<MovimientoAtmDTO> ultimos3Movimientos; // Solo para impresión rápida
}