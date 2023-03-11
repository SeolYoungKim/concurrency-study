package com.example.stock.facade;

import com.example.stock.service.OptimisticLockStockService;
import org.springframework.stereotype.Service;

// OptimisticLock 은 실패 시 재시도 해줘야 함
@Service
public class OptimisticLockStockFacade {
    private OptimisticLockStockService optimisticLockStockService;

    public OptimisticLockStockFacade(final OptimisticLockStockService optimisticLockStockService) {
        this.optimisticLockStockService = optimisticLockStockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }

    }
}
