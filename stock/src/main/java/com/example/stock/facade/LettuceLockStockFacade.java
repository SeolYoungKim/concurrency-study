package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockServiceForLock;
import org.springframework.stereotype.Component;

// Lettuce
// setnx 명령어를 이용해 Mysql의 Named Lock과 비슷한 효과를 얻을 수 있음
// 스레드 하나가 특정 key 값을 설정하면, 나머지 스레드는 값을 설정할 수 없음 (Named Lock에서의 get_lock)
// del 명령어를 통해 key 값을 삭제하면, 그제서야 다른 스레드가 값을 설정할 수 있게 됨 (Named lock에서의 release_lock)
// 세션 관리는 신경쓰지 않아도 된다는 장점
// 구현이 간단하나, 스핀락 방식이기 때문에 Redis 서버에 부하를 줄 수 있다. -> Thread.sleep()을 활용하여 락 획득 재시도 간에 텀이 필요함
@Component
public class LettuceLockStockFacade {
    private final RedisLockRepository redisLockRepository;
    private final StockServiceForLock stockService;

    public LettuceLockStockFacade(final RedisLockRepository redisLockRepository,
            final StockServiceForLock stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) throws InterruptedException {
        while (!redisLockRepository.lock(key)) {  // 락 획득에 성공하면 해당 로직을 수행하지 않음. 락 획득 실패 시 대기
            Thread.sleep(100);
        }

        try {
            stockService.decrease(key, quantity);
        } finally {
            redisLockRepository.unlock(key);
        }
    }
}
