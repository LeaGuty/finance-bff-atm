package cl.duoc.finance_bff_atm.service;

import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;

/**
 * Interfaz del servicio principal del BFF ATM.
 *
 * Define las operaciones de negocio disponibles para el cajero automatico.
 * La implementacion se encarga de orquestar las llamadas al backend principal,
 * aplicar reglas de seguridad (enmascaramiento de datos) y formatear las
 * respuestas para la interfaz del ATM.
 *
 * @author Duoc UC - Backend 3
 */
public interface FinanceAtmService {

    /**
     * Consulta el saldo de una cuenta y retorna un resumen seguro para el cajero.
     *
     * Orquesta dos llamadas al backend principal:
     * 1. GET /api/v1/cuentas/{id} -> obtiene nombre y saldo
     * 2. GET /api/v1/cuentas/{id}/transacciones -> obtiene movimientos
     *
     * Aplica enmascaramiento al nombre del cliente y limita los movimientos
     * a los ultimos 3 para impresion en voucher.
     *
     * @param id identificador de la cuenta bancaria
     * @return ResumenCajeroDTO con datos formateados para el ATM
     */
    ResumenCajeroDTO consultarSaldoSeguro(Long id);
}
