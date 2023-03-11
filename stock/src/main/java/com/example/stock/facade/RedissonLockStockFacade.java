package com.example.stock.facade;

import com.example.stock.service.StockServiceForLock;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

// Redisson
// 자신이 점유하고 있는 락을 해제할 때 채널에 메세지를 보내줌으로써 락을 획득해야하는 스레드들에게 락 획득을 하라고 전달해줌
// 락 획득을 해야하는 스레드가 메세지를 받으면 락 획득을 시도함
// 락 해제가 되었을 때 1회 혹은 몇회만 시도하기 떄문에 레디스 부하를 줄여줌
// pub-sub 기반의 구현 -> Redis 부하 감소
// 구현이 복잡하고, 별도의 라이브러리를 사용해야 함
@Component
public class RedissonLockStockFacade {
    private final RedissonClient redissonClient;
    private final StockServiceForLock stockServiceForLock;

    public RedissonLockStockFacade(final RedissonClient redissonClient,
            final StockServiceForLock stockServiceForLock) {
        this.redissonClient = redissonClient;
        this.stockServiceForLock = stockServiceForLock;
    }

    public void decrease(Long key, Long quantity) {
        final RLock lock = redissonClient.getLock(key.toString());

        try {
            final boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("lock 획득 실패");
                return;
            }

            stockServiceForLock.decrease(key, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
