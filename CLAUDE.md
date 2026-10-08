# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

배달 주문 서비스 백엔드 API (온보딩 개인 과제). 요구사항의 원본은 `docs/20261006 과제 발제 자료.md`(로컬 전용, `.gitignore`로 레포에서 제외)이며, 기능 구현 시 해당 문서의 **PART 2(요구사항)·5-1(테스트 시나리오)·5-2(코드 점검 체크리스트)** 를 기준으로 판단한다. 요구사항에 없는 부분은 스스로 판단해 채우되, 문서의 상태 코드·권한 규칙과 어긋나지 않게 한다.

- Java 21 · Spring Boot 4.1.1 · Gradle(Groovy) · PostgreSQL 18 · Spring Data JPA · Spring Security · Validation · Lombok
- JWT는 JJWT 0.13, 테스트 DB는 Testcontainers 2.x(`org.testcontainers.postgresql.PostgreSQLContainer`)
- 모놀리식 + **4계층 레이어드 아키텍처**(presentation · application · domain · infrastructure). 발제 자료는 3 Layer를 제시하지만, 레이어드 아키텍처 학습을 목적으로 4계층으로 진행한다. 헥사고날·클린 아키텍처는 적용하지 않는다.

## 개발 방식: TDD

**모든 구현은 TDD로 진행한다. 프로덕션 코드보다 테스트 코드를 먼저 작성한다.**

1. **Red:** 구현할 동작을 검증하는 실패하는 테스트를 먼저 작성하고, 실제로 실패하는지 실행해 확인한다
2. **Green:** 테스트를 통과시키는 최소한의 프로덕션 코드를 작성한다
3. **Refactor:** 테스트가 통과하는 상태를 유지하며 코드를 정리한다

- 테스트 없이 프로덕션 코드를 먼저 작성하지 않는다. 기능 요청을 받으면 테스트 작성부터 시작한다
- 테스트 케이스는 `docs/design/01-requirements.md`의 기능별 규칙과 실패 조건(상태 코드)을 근거로 도출한다
- 상세 전략은 `docs/design/07-test-strategy.md`

### 테스트 전략 요약

기능 하나를 **안에서 바깥으로** 진행한다: ① 도메인(엔티티) → ② 도메인 서비스·Facade → ③ Repository(직접 만든 Query Method가 있을 때만) → ④ E2E

| 대상 | 방식 | 검증 범위 |
|---|---|---|
| domain | 순수 JUnit (Spring·DB 없음) | 상태 전이와 409, 총액 계산, 소유 확인 |
| domain 서비스 | Mockito 단위 테스트. Mock은 Repository만, **엔티티는 진짜 객체** | 404 → 403 → 409 검증 순서, 저장 여부 |
| application (Facade) | Mockito 단위 테스트. Mock은 도메인 서비스·`PasswordEncoder`·`TokenProvider`. **여러 도메인을 조율하거나 기술을 엮는 Facade만** 작성 | 호출 순서, 기술 처리(암호화·토큰), 응답 DTO |
| repository | `@DataJpaTest` + Testcontainers | 직접 이름 지은 Query Method만 |
| API | E2E: `@SpringBootTest` + MockMvc + Testcontainers | 성공 응답, 401·403(역할)·400, 대표 404·409 |

- 테스트 DB는 **Testcontainers PostgreSQL** (H2 금지). Docker Desktop이 켜져 있어야 한다
- 테스트 설정은 `application-test.yml` + `@ActiveProfiles("test")`. `src/test/resources/application.yml`을 만들면 main 설정을 통째로 가리므로 금지
- E2E에 `@Transactional` 롤백을 쓰지 않는다 (지연 로딩·제약 위반을 숨김). 각 테스트 전 테이블을 비운다
- 규칙은 가장 안쪽 계층에서 검증하고, 바깥 계층은 대표 케이스만 검증한다

### 테스트 코드 작성 원칙

**형식**
- 테스트 클래스는 프로덕션 클래스와 같은 패키지에 `{클래스명}Test`로 둔다. E2E는 `{도메인}/presentation/{도메인}ApiTest`
- `@DisplayName`에 **한국어 문장**으로 기대 동작을 적는다. 예: `"결제완료가 아닌 주문은 수락할 수 없다"`
- 본문은 `// given` · `// when` · `// then`으로 나눈다
- 검증은 AssertJ(`assertThat`, `assertThatThrownBy`). 예외는 타입뿐 아니라 **`ErrorCode`까지** 확인한다

**내용**
- 테스트 하나는 **동작 하나**만 검증한다
- 성공 케이스만큼 **실패 케이스**를 쓴다. 01의 기능별 실패 조건(상태 코드)마다 대응하는 테스트가 있어야 한다
- **구현이 아니라 동작을 검증한다.** 반환값·상태 변화를 먼저 확인하고, Mockito `verify`는 호출 여부 자체가 요구사항일 때만 쓴다 (예: 검증 실패 시 `save`가 호출되지 않음)
- 기대값은 **리터럴**로 적는다. 테스트 안에서 프로덕션 로직을 다시 계산하지 않는다 (예: `price * quantity` ❌, `7000` ⭕). 테스트에 `if`·`for` 같은 분기·반복을 넣지 않는다
- 테스트끼리 **독립적**이어야 한다. 실행 순서나 다른 테스트가 남긴 데이터에 의존하지 않는다
- 결과가 **항상 같아야** 한다. 현재 시각·난수에 의존하는 코드는 `Clock` 등을 주입받게 만들어 테스트에서 고정한다 (예: JWT 만료)

**픽스처와 프로덕션 코드**
- 반복되는 엔티티 생성은 도메인별 픽스처 클래스(예: `MenuFixture`)로 모으고, 테스트에서는 **그 테스트에 중요한 값만** 드러낸다
- 테스트를 위해 프로덕션 코드에 setter나 `public` 생성자를 추가하지 않는다. ID 설정 등은 픽스처에서 `ReflectionTestUtils`로 처리한다

**TDD 진행**
- Red 단계에서 테스트가 **기대한 이유로** 실패하는지 확인한다 (컴파일 에러가 아니라 단언 실패)
- 테스트를 통과시키려고 테스트를 고치지 않는다. 요구사항이 바뀐 것이면 `01-requirements.md`를 먼저 고치고 테스트를 고친다

## 명령어

```bash
docker compose up -d                     # DB 켜기 (.env 필요, 없으면 cp .env.example .env)
docker compose down -v                   # DB 초기화 (enum CHECK 제약 갱신 등)
set -a; source .env; set +a              # 셸에 환경 변수 로드 (gradlew 실행 전 필수)
./gradlew bootRun                        # 실행
./gradlew build                          # 빌드 + 테스트
./gradlew test                           # 전체 테스트
./gradlew test --tests "com.example.delivery.menu.domain.MenuServiceTest"                 # 단일 클래스
./gradlew test --tests "com.example.delivery.menu.domain.MenuServiceTest.methodName"      # 단일 메서드
```

테스트는 Testcontainers로 DB를 띄우므로 Docker Desktop만 켜져 있으면 `.env` 없이 통과한다. 공통 설정은 `src/test/java/.../support`(`TestcontainersConfig`, E2E 부모 `ApiTestSupport`, Repository 부모 `RepositoryTestSupport`, 두 부모가 테스트 전에 호출하는 `DatabaseCleaner`).

## 아키텍처

### 4계층 구성

패키지는 **도메인별로 먼저, 그 안을 계층별로** 나눈다(04 D-28). `user`·`menu`·`order`·`payment` 각각이 아래 네 패키지를 가지며, 여러 도메인이 공유하는 것(BaseEntity, Security 설정, JWT, 공통 예외, `PageResponse`)은 `global` 아래에 같은 계층 이름으로 둔다.

| 계층 | 담는 것 | 하지 않는 것 |
|---|---|---|
| `presentation` | Controller, 요청 DTO(`XxxRequest`, `@Valid` 검증, `toCommand()`), 상태 코드 결정 | 비즈니스 판단, Repository 호출, 응답 DTO 정의 |
| `application` | Facade(`XxxFacade`): `@Transactional`, 도메인 서비스 호출 순서 조율, 기술 처리(`PasswordEncoder`·`TokenProvider` — 인터페이스에만 의존, 04 D-33), `dto/`의 입력 `XxxCommand`·출력 `XxxResponse`, 엔티티 → 응답 DTO 변환 | Repository 직접 호출, 상태 전이·금액 계산 같은 규칙 구현, presentation import |
| `domain` | Entity(비즈니스 메서드 포함), enum, 도메인 서비스(`XxxService`: 조회·404·403·저장, 엔티티에 일 시키기), Repository **순수 인터페이스**(도메인 언어로 이름 지은 메서드), 도메인 예외 | Spring Web·Security·JWT·Spring Data JPA 의존 (`JpaRepository` 상속 금지, 페이징 결과 `Page`만 허용) |
| `infrastructure` | DB 접근 구현(`XxxJpaRepository extends JpaRepository` + domain 인터페이스를 구현하는 `XxxRepositoryImpl`), JWT 발급·검증, Security 필터·설정, 외부 기술 구현 | 비즈니스 규칙 |

의존 방향은 presentation → application → domain 한 방향. infrastructure는 domain을 참조(Repository 인터페이스 구현)할 수 있지만 domain은 infrastructure를 모른다(04 D-29). Controller는 Facade만, Facade는 도메인 서비스만, 도메인 서비스는 자기 도메인의 Repository 인터페이스만 주입받는다(04 D-31). Spring Data JPA는 infrastructure 안에만 둔다. application은 presentation을 import하지 않는다 — Controller가 `request.toCommand()`로 넘기고 Facade가 돌려준 Response를 그대로 응답한다(04 D-30). 도메인 서비스는 DTO·트랜잭션·Security를 모른다. presentation이 Repository를 직접 호출하지 않는다 (과제 체크리스트 "Controller → Service → Repository"는 이 구조에서도 유지됨).

### 도메인 규칙은 엔티티에

비즈니스 규칙은 서비스가 아니라 **엔티티 메서드**에 둔다. 도메인 서비스는 조회·존재·소유 확인·저장과 엔티티 호출만, Facade는 순서 조율만 하도록 얇게 유지한다.
- 상태 전이: `order.pay()` · `order.cancel()` · `order.accept()` · `order.complete()` — 허용되지 않는 전이는 엔티티가 `BusinessException(INVALID_ORDER_STATUS)`(409)로 거절
- 소유 확인: `menu.isOwnedBy(userId)` · `order.isOrderedBy(userId)` · `order.isMenuOwnedBy(userId)` — 엔티티는 boolean만 제공하고, 403·404 예외는 도메인 서비스가 던진다 (여러 Facade가 재사용)
- 생성은 정적 팩토리: `Order.create(customer, menu, quantity, deliveryAddress)`에서 스냅샷(`menuName`, `unitPrice`) 복사와 총액 계산, `Payment.complete(order, method)`는 금액을 `order.getTotalPrice()`에서 가져온다
- 결제는 도메인 서비스 `PaymentService.pay(order, method)`가 순서대로 지시: `order.pay()` → `Payment.complete(order, method)` → 저장. 주문 조회(404→403)는 `PaymentFacade`가 `OrderService`로 받아 넘긴다. Order는 Payment를 모른다
- 현재 시각이 필요한 메서드는 시각을 인자로 받는다 (`menu.delete(LocalDateTime)`). 도메인 서비스가 주입받은 `Clock`으로 만든다
- Facade·도메인 서비스 메서드는 `AuthUser`가 아니라 `userId`·`UserRole`을 받는다
- 트랜잭션은 Facade 메서드 단위(`@Transactional`, 조회는 `readOnly = true`). 도메인 서비스는 트랜잭션을 열지 않는다. 엔티티 → DTO 변환도 Facade 안에서 끝낸다. `spring.jpa.open-in-view: false`
- `JwtAuthenticationFilter`는 토큰이 없거나 잘못돼도 **거절하지 않고** 인증 정보 없이 다음 필터로 넘긴다. 거절은 `AuthorizationFilter`(Security URL 규칙)가 한다. 흐름은 `docs/design/05-sequence-diagram.md`

JPA 엔티티와 도메인 모델을 분리하지 않는다 (별도 도메인 객체·매퍼 없음). JPA 엔티티가 곧 도메인 모델이다. 상세는 `docs/design/02-domain.md`(ERD·테이블 명세), `docs/design/04-class-diagram.md`.

### 도메인 관계

모두 `@ManyToOne(fetch = FetchType.LAZY)` (기본값이 EAGER이므로 반드시 명시):
- Menu → User(OWNER), Order → User(CUSTOMER), Order → Menu, Payment → Order
- 단방향만 매핑한다. `@OneToMany` 컬렉션을 두지 않고, 목록은 Repository로 조회한다
- Payment는 한 주문에 여러 건 쌓일 수 있으나 "결제 완료"는 한 번만 가능 — 주문 상태 검사 + `Order`의 `@Version` 낙관적 락으로 보장. 충돌 예외(`ObjectOptimisticLockingFailureException`)는 409(`CONCURRENT_MODIFICATION`)로 변환한다
- 가게 엔티티 없음 (사장님 = 가게), 주문 1건 = 메뉴 1개 + 수량
- 테이블: `users`, `menus`, `orders`, `payments`. 금액은 `Long`(BIGINT). 메뉴 Soft Delete는 `deleted_at`(NULL이면 삭제 안 됨)
- 메뉴는 판매 상태 `status`(`ON_SALE`·`SOLD_OUT`)를 `deleted_at`과 별도로 가진다. **상태 변경 API는 만들지 않는다**(확장 대비 컬럼). 품절 메뉴도 목록에 보이고, 주문하면 `Order.create()`가 409(`MENU_SOLD_OUT`)로 거절한다 (삭제 404 확인이 먼저)
- 회원은 `status`(`ACTIVE`·`WITHDRAWN`) + `withdrawn_at`을 가진다. **탈퇴 API는 만들지 않는다**(확장 대비 컬럼). 로그인은 `ACTIVE`만 허용하고 아니면 401, 탈퇴 회원 아이디도 재사용 불가(409)

주문 상태 전이 (이 외의 전이는 409로 거절):
- CUSTOMER: `ORDERED → PAID`(결제 API를 통해서만), `ORDERED → CANCELED`
- OWNER: `PAID → ACCEPTED`, `ACCEPTED → COMPLETED`

### API 규칙

명세는 `docs/design/03-api-spec.md`. 구현 시 URL·필드·상태 코드를 임의로 바꾸지 말고, 바꿔야 하면 03·04 문서를 같은 PR에서 갱신한다.
- 인증 `POST /api/auth/signup`·`/login`, 결제 `POST /api/orders/{orderId}/payments`, 상태 변경 `PATCH /api/orders/{orderId}/{cancel|accept|complete}`, 메뉴 수정은 `PUT`(전체 교체)
- 성공 응답은 껍데기 없이 데이터 그대로. 생성 201, 상태 변경은 200 + 변경된 `OrderResponse`, 메뉴 삭제 204
- 메뉴 목록은 `PageResponse`(직접 정의, Spring `Page`를 그대로 반환하지 않음), 기본 size 10·최대 50, 최신 등록순 고정
- 비밀번호 8~20자 (BCrypt 72바이트 제한), 문자열 `@Size`는 02의 컬럼 길이와 일치
- 주문 응답의 `customerUsername`은 목록 조회에서 `@EntityGraph(attributePaths = "customer")`로 함께 조회 (N+1 방지)

### 예외 처리

- 비즈니스 규칙 위반은 `throw new BusinessException(ErrorCode.XXX)`로 던지고, `global/presentation/GlobalExceptionHandler`가 `ErrorResponse`로 변환한다
- 도메인별 에러 코드는 `global/domain/exception/ErrorCode`에 추가한다. domain이 Spring Web에 의존하지 않도록 `HttpStatus`가 아닌 `int status`를 쓴다
- 상태 흐름 규칙 위반(이미 결제된 주문 결제, 결제 후 취소 등)은 **409로 통일**

## 반드시 지킬 규칙 (과제 요구사항)

- 모든 엔티티는 `@MappedSuperclass` BaseEntity를 상속해 생성·수정 시각을 JPA Auditing으로 기록 (`@EnableJpaAuditing` 필요)
- 테이블 이름에 `user`, `order` 사용 금지 (PostgreSQL 예약어) → `@Table(name = "users")` 처럼 복수형으로 명시
- enum은 `@Enumerated(EnumType.STRING)`
- 필수 컬럼 `nullable = false`, 로그인 아이디 `unique = true` — 서비스 검사와 별개로 DB 제약도 건다
- 메뉴 삭제는 Soft Delete. 삭제된 메뉴는 목록에서 제외, 단건 조회·수정·삭제·주문에서는 404. 기존 주문 기록은 유지
- 요청·응답은 DTO로만. Entity를 그대로 응답하지 않으며 응답에 비밀번호를 담지 않는다
- domain enum은 application 밖으로 내보내지 않는다(04 D-32). Request·Command·Response의 enum 값은 `String` — 받을 때는 Request의 `@Pattern`으로 검증하고 Facade가 `valueOf()`, 내보낼 때는 `Response.from()`에서 `name()`. enum 값을 바꾸면 `@Pattern`도 함께 고친다
- 주문 총액(메뉴 가격 × 수량)과 결제 금액은 **서버가 계산** — 요청에서 금액을 받지 않음
- 메뉴 주인·주문자 등 "누가"는 요청 본문이 아니라 **JWT에서** 꺼낸다
- 역할별 주문 목록·아이디 중복 확인은 Spring Data JPA Query Methods로 구현
- 비밀번호는 `BCryptPasswordEncoder`를 **직접** 빈으로 등록한다. 기본 위임 인코더(`createDelegatingPasswordEncoder`)는 `{bcrypt}` 접두사를 붙여 발제 5-1 ⑥ "`$2`로 시작하는 BCrypt 값" 확인에 실패한다
- JWT 클레임은 `sub`(회원 PK) · `username` · `role` · `exp`만. 필터는 클레임으로 DB 조회 없이 `AuthUser(userId, username, role)`를 만든다 (만료 1시간, 로그인 응답 본문으로 전달, HS256 비밀키 32바이트 이상)
- 동시 가입으로 UNIQUE 제약 위반(`DataIntegrityViolationException`)이 나면 409(`DUPLICATE_USERNAME`)로 변환한다

## Spring Security 함정 (발제 3-5)

- `/error`를 `permitAll()` 하지 않으면 400/404/409/500 등이 모두 빈 403으로 바뀐다
- JWT API이므로 `csrf.disable()` — 안 하면 POST/PATCH/DELETE가 전부 403
- 토큰 없는 요청은 기본적으로 403 (401 구분은 도전 기능)
- `ddl-auto: update`는 기존 CHECK 제약을 갱신하지 않으므로, enum 값을 추가하면 한 번 `create`로 재생성해야 한다

## 설정 · 비밀값

`src/main/resources/application.yml`은 git에 추적되며 레포는 Public으로 제출된다. DB 비밀번호·JWT 비밀키는 하드코딩하지 말고 환경변수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` — README 기준)로 주입한다. `application.properties`와 yml을 함께 두지 않는다 (properties가 우선해 yml 변경이 무시됨).

## Git 워크플로

GitHub Flow. `main`에서 `<type>/<desc>` 브랜치를 따고 PR → Squash merge. 한 번에 한 브랜치만 진행하며 순서는 `docs/design` → `chore/init-setup` → `feat/auth` → `feat/menu` → `feat/order` → `feat/payment` → `feat/challenge-*`. 커밋 메시지는 `feat: 메뉴 등록 API 구현` 형식 (type: feat·fix·refactor·docs·chore·test). 상세는 README의 "브랜치 전략" 참고.
