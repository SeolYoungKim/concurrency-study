package com.example.stock.facade;

import com.example.stock.repository.LockRepository;
import com.example.stock.service.StockServiceForLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NamedLockStockFacade {
    private final LockRepository lockRepository;
    private final StockServiceForLock stockService;

    public NamedLockStockFacade(final LockRepository lockRepository,
            final StockServiceForLock stockService) {
        this.lockRepository = lockRepository;
        this.stockService = stockService;
    }

    @Transactional
    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(id.toString());
            stockService.decrease(id, quantity);
        } finally {
            lockRepository.releaseLock(id.toString());
        }
    }
}
