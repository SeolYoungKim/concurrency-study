package com.example.stock.repository;

import com.example.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Named Lock을 사용할 때는 데이터 소스를 분리하여 사용하는 것을 추천한다.
// 같은 데이터 소스를 사용하게 되면, 커넥션 풀이 부족해지는 현상으로 인해 다른 서비스에도 영향을 끼칠 수 있기 때문이다.
// 실무에서는 데이터 소스를 분리하여 사용할 것을 추천!
// Named Lock은 주로 분산락을 구현할 때 사용함
// 타임아웃을 구현하기가 쉬움
// 데이터 삽입 시 정합성을 맞추는 경우에도 사용
// 트랜잭션 종료 시 락 해제와 세션 관리를 제대로 잘 해줘야 함. 실무에서는 구현 방법이 복잡할 수 있음
public interface LockRepository extends JpaRepository<Stock, Long> {
    @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
    void getLock(String key);

    @Query(value = "select release_lock(:key)", nativeQuery = true)
    void releaseLock(String key);
}
