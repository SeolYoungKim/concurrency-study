package com.example.stock.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockServiceTest {
    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;


    @BeforeEach
    void setUp() {
        final Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    void tearDown() {
        stockRepository.deleteAllInBatch();
    }

    @Test
    void stock_decrease() throws InterruptedException {
        stockService.decrease(1L, 1L);
        final Stock stock = stockRepository.findById(1L).orElseThrow();

        assertThat(stock.getQuantity()).isEqualTo(99L);
    }

    @Test
    void 동시에_100개의_요청이_들어오는_경우() throws InterruptedException {
        int threadCount = 100;
        final ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 100개의 요청이 끝날 때 까지 기다린다
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        final Stock stock = stockRepository.findById(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0L);
    }

    /**
     * 한 스레드 내에서 수행된 flush()는 해당 스레드에서만 적용되는 것 같다. <br/>
     * flush()도 엔티티 매니저, 즉 영속성 컨텍스트 단위로 적용되는 것일까? <br/>
     * - flush()를 호출했을 때, 해당 영속성 컨텍스트와 db는 동기화된다. <br/>
     * - 단일 스레드 내에서는 영속성 컨텍스트를 비우고 값을 조회하면 변경된 값이 조회된다. <br/>
     * - flush()만 수행하고 Transaction commit 을 수행하지 않았을 때, 해당 시점에 다른 스레드들이 접근할 경우, 변경되기 전의 값이 조회된다. <br/>
     * - 여기서 추측을 해볼 수 있는데, "flush()를 수행하면 DB와 동기화가 된다"는 기능은 "단일 스레드 내에서만" 적용되는 말인 것 같다. 즉, DB의 값은 변경되지 않는 것이다.<br/>
     * - DB의 실행 단위는 "Transaction"이다. 그러니 커밋이 되어야만 데이터가 변경될 것이다.<br/>
     * - 그리고 엔티티 매니저와 영속성 컨텍스트는 다른 스레드와 공유되지 않을 것이다. (자세한 내용은 찾아보자.) 따라서, flush() 내역은 다른 스레드에서 조회할 수 없다. <br/><br/>
     *
     * 결론적으로, flush()만 수행했을 때 "DB와 동기화"된다는 의미는 "논리적 동기화"인 것 같다. 단일 스레드 내에서 "DB에 동기화된 것 처럼" 접근할 수 있다는 의미인 것 같다는 생각이 든다.
     * 즉, DB와 "물리적으로 동기화"되기 위해서는 "Commit"이 발생되어야만 한다.
     * 또한, 엔티티 매니저와 영속성 컨텍스트는 스레드 간에 공유되지 않는다.
     * 즉, 한 스레드가 엔티티 매니저와 영속성 컨텍스트를 이용해 엔티티를 변경하고 flush()를 수행해도, 이는 DB에 반영되지 않을 뿐만 아니라
     * 다른 스레드는 해당 엔티티가 수행한 변경과 flush() 내역에 접근할 수 없다.<br/>
     *
     * 따라서, 비즈니스 메서드에 synchronized 키워드를 붙이고 변경을 수행 해봤자, commit이 수행되기 전에 다른 스레드가 해당 데이터에 접근 해버리면 동시성 이슈로부터 안전할 수 없다.
     * 사실 이 모든 이슈는 @Transactional 때문인데, 트랜잭션 AOP가 적용되면 Proxy 객체를 만들게 되고, 이는 여러 스레드가 동시에 접근할 수 있기 때문이다. <br/>
     * 더 자세한 내용은 한번 블로깅을 해보자.
     *
     */
    @Test
    void 서로_다른_스레드는_flush된_값을_어떻게_읽어올까() throws InterruptedException {
        stockService.decreaseForTest(1L, 1L);
    }
}