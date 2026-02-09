package cl.duoc.finance_bff_atm.service;

import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;

public interface FinanceAtmService {
    ResumenCajeroDTO consultarSaldoSeguro(Long id);
}