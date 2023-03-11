package com.example.stock.repository;

import com.example.stock.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {
    // Pessimistic Lock
    // 충돌이 빈번할 경우 Optimistic Lock보다 좋을 수 있으나, 기본적으로 성능 감소가 수반되는 방법임
    // Pessimistic Lock은 Stock 엔티티 자체에 락을 건다.
    // 타임아웃을 구현하기 힘듬
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdWithPessimisticLock(Long id);

    // Optimistic Lock
    // 별도의 Lock을 잡지 않아 성능상 이점이 있으나, 업데이트 실패 시 재시도 로직을 작성해줘야 한다는 단점이 있음
    // 충돌이 빈번하게 일어날 경우, 혹은 예상되는 경우에는 Pessimistic Lock을 이용하는게 성능상으로는 더 좋다!
    @Lock(value = LockModeType.OPTIMISTIC)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdWithOptimisticLock(Long id);

    // Named Lock
    // Named Lock은 엔티티에 락을 거는게 아닌, 별도의 공간에 락을 건다.
}
