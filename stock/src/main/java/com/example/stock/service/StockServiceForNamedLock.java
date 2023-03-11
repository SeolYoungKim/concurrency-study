package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockServiceForNamedLock {
    private final StockRepository stockRepository;

    public StockServiceForNamedLock(final StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    // 락의 트랜잭션과 별개로 수행되어야 함
    // 해당 로직이 롤백된다고 해서 락을 반환하는 데 실패하면 안되기 때문
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void decrease(Long id, Long quantity) {
        final Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);  // 즉시 flush
    }
}
