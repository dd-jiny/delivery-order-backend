package com.example.delivery.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.order.domain.Order;
import com.example.delivery.support.ApiTestSupport;
import com.example.delivery.user.domain.UserRole;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 동시 결제 E2E는 두 요청이 실제로 겹칠지 타이밍에 달려 있다. 여기서는 트랜잭션 두 개를 일부러 겹치게 만들어
 * Order의 @Version이 나중에 커밋하는 쪽을 항상 막는지, 그 예외가 GlobalExceptionHandler가 409로 바꾸는 타입인지 확인한다 (02 D-04).
 * 실제 커밋이 필요해 롤백되는 @DataJpaTest 대신 전체 앱을 띄운다.
 */
class OrderOptimisticLockTest extends ApiTestSupport {

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("같은 주문을 먼저 읽은 트랜잭션이 나중에 커밋하면 낙관적 락 예외로 롤백된다")
    void staleUpdateIsRejected() throws Exception {
        // given
        String owner = signupAndLogin("owner1", UserRole.OWNER);
        String customer = signupAndLogin("cust1", UserRole.CUSTOMER);
        long orderId = createOrderId(customer, createMenuId(owner, "김밥", 3500), 2);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // when & then
        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            Order stale = orderJpaRepository.findById(orderId).orElseThrow();
            inner.executeWithoutResult(s -> orderJpaRepository.findById(orderId).orElseThrow().cancel());
            stale.pay();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, version FROM orders WHERE id = ?", orderId);
        assertThat(row).containsEntry("status", "CANCELED").containsEntry("version", 1L);
    }
}
