package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
    private final StockRepository stockRepository;

    public StockService(final StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    // synchronized는 하나의 프로세스 안에서만 스레드 안전이 보장된다. -> 서버가 2대 이상일 경우는 동시성 해결을 해주지 못함
//    @Transactional  // 트랜잭션 AOP는 스레드 안전하지 않다?
    public synchronized void decrease(Long id, Long quantity) throws InterruptedException {
        // get stock
        final Stock stock = stockRepository.findById(id).orElseThrow();

        // decrease quantity
        // 트랜잭션이 종료되기 전에, 트랜잭션 proxy에 다른 스레드가 진입하여 decrease 메서드를 호출 해버릴 수 있음
        // 그럼 동시에 접근한것과 같은상태가 됨. 즉, 변경사항이 반영되지 않은 상태를 다른 스레드가 읽어버리게 되는 것!
        stock.decrease(quantity);

        // save
//        stockRepository.save(stock);  // Transaction이 끝날 때 flush
        stockRepository.saveAndFlush(stock);  // 즉시 flush
    }

    // synchronized는 하나의 프로세스 안에서만 스레드 안전이 보장된다. -> 서버가 2대 이상일 경우는 동시성 해결을 해주지 못함
    @Transactional  // 트랜잭션 AOP는 스레드 안전하지 않다?
    public synchronized void decreaseForTest(Long id, Long quantity) throws InterruptedException {
        // get stock
        final Stock stock = stockRepository.findById(id).orElseThrow();
        System.out.println("[ " + Thread.currentThread().getName() + " ]" + "[Before Decrease] " + stock.getQuantity());

        // decrease quantity
        // 트랜잭션이 종료되기 전에, 트랜잭션 proxy에 다른 스레드가 진입하여 decrease 메서드를 호출 해버릴 수 있음
        // 그럼 동시에 접근한것과 같은상태가 됨. 즉, 변경사항이 반영되지 않은 상태를 다른 스레드가 읽어버리게 되는 것!
        stock.decrease(quantity);
        System.out.println("[ " + Thread.currentThread().getName() + " ]" + "[After Decrease] " + stock.getQuantity());

        // save
//        stockRepository.save(stock);  // Transaction이 끝날 때 flush
        stockRepository.saveAndFlush(stock);  // 즉시 flush
        System.out.println("[ " + Thread.currentThread().getName() + " ]" + "[After saveAndFlush] " + stock.getQuantity());

        final ExecutorService executorService = Executors.newFixedThreadPool(32);
        final CountDownLatch countDownLatch = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                try {
                    System.out.println("[ " + Thread.currentThread().getName() + " ]" + stockRepository.findById(1L).get().getQuantity());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        System.out.println("[ " + Thread.currentThread().getName() + " ] 종료");
    }
}
