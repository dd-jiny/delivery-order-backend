# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

배달 주문 서비스 백엔드 API (온보딩 개인 과제). 요구사항의 원본은 `docs/20261006 과제 발제 자료.md`이며, 기능 구현 시 해당 문서의 **PART 2(요구사항)·5-1(테스트 시나리오)·5-2(코드 점검 체크리스트)** 를 기준으로 판단한다. 요구사항에 없는 부분은 스스로 판단해 채우되, 문서의 상태 코드·권한 규칙과 어긋나지 않게 한다.

- Java 21 · Spring Boot 4.1.1 · Gradle(Groovy) · PostgreSQL 18 · Spring Data JPA · Spring Security · Validation · Lombok
- JWT 라이브러리(JJWT 등)는 아직 `build.gradle`에 없음 — 인증 구현 시 추가 필요
- 모놀리식 + **4계층 레이어드 아키텍처**(presentation · application · domain · infrastructure). 발제 자료는 3 Layer를 제시하지만, 레이어드 아키텍처 학습을 목적으로 4계층으로 진행한다. 헥사고날·클린 아키텍처는 적용하지 않는다.

## 명령어

```bash
docker compose up -d                     # DB 켜기 (.env 필요, 없으면 cp .env.example .env)
docker compose down -v                   # DB 초기화 (enum CHECK 제약 갱신 등)
set -a; source .env; set +a              # 셸에 환경 변수 로드 (gradlew 실행 전 필수)
./gradlew bootRun                        # 실행
./gradlew build                          # 빌드 + 테스트
./gradlew test                           # 전체 테스트
./gradlew test --tests "com.example.delivery.menu.application.MenuServiceTest"            # 단일 클래스
./gradlew test --tests "com.example.delivery.menu.application.MenuServiceTest.methodName" # 단일 메서드
```

`DeliveryApplicationTests`(`@SpringBootTest`)는 전체 컨텍스트를 띄우므로 PostgreSQL이 실행 중이어야 통과한다.

## 아키텍처

### 4계층 구성

패키지는 **도메인별로 먼저, 그 안을 계층별로** 나눈다. `user`·`menu`·`order`·`payment` 각각이 아래 네 패키지를 가지며, 여러 도메인이 공유하는 것(BaseEntity, Security 설정, JWT, 공통 예외)은 `global` 아래에 같은 계층 이름으로 둔다.

| 계층 | 담는 것 | 하지 않는 것 |
|---|---|---|
| `presentation` | Controller, 요청·응답 DTO, `@Valid` 검증, 상태 코드 결정 | 비즈니스 판단, Repository 호출 |
| `application` | Service, `@Transactional`, 유스케이스 흐름(조회 → 도메인 메서드 호출 → 저장), 엔티티 ↔ 응답 DTO 변환 | 상태 전이·금액 계산 같은 규칙 자체를 구현하지 않음 |
| `domain` | Entity(비즈니스 메서드 포함), enum, Repository 인터페이스(`JpaRepository` 상속), 도메인 예외 | Spring Web·Security·JWT 의존 |
| `infrastructure` | JWT 발급·검증, Security 필터·설정, 외부 기술 구현(필요 시 QueryDSL 등) | 비즈니스 규칙 |

의존 방향은 presentation → application → domain 한 방향. infrastructure는 domain을 참조할 수 있지만 domain은 infrastructure를 모른다. presentation이 domain Repository를 직접 호출하지 않는다 (과제 체크리스트 "Controller → Service → Repository"는 이 구조에서도 유지됨).

### 도메인 규칙은 엔티티에

비즈니스 규칙은 Service가 아니라 **엔티티 메서드**에 둔다. Service는 얇게 유지한다.
- 상태 전이: `order.pay()` · `order.cancel()` · `order.accept()` · `order.complete()` — 허용되지 않는 전이는 엔티티가 예외로 거절
- 소유 확인: `menu.isOwnedBy(userId)` · `order.isOrderedBy(userId)`
- 생성 규칙: `Order.create(customer, menu, quantity, address)`에서 총액 계산, `menu.delete()`로 Soft Delete

JPA 엔티티와 도메인 모델을 분리하지 않는다 (별도 도메인 객체·매퍼 없음). JPA 엔티티가 곧 도메인 모델이다.

### 도메인 관계

모두 `@ManyToOne(fetch = FetchType.LAZY)` (기본값이 EAGER이므로 반드시 명시):
- Menu → User(OWNER), Order → User(CUSTOMER), Order → Menu, Payment → Order
- Payment는 한 주문에 여러 건 쌓일 수 있으나 "결제 완료"는 한 번만 가능
- 가게 엔티티 없음 (사장님 = 가게), 주문 1건 = 메뉴 1개 + 수량

주문 상태 전이 (이 외의 전이는 400 또는 409 중 하나로 **일관되게** 거절):
- CUSTOMER: `ORDERED → PAID`(결제 API를 통해서만), `ORDERED → CANCELED`
- OWNER: `PAID → ACCEPTED`, `ACCEPTED → COMPLETED`

### 예외 처리

- 비즈니스 규칙 위반은 `throw new BusinessException(ErrorCode.XXX)`로 던지고, `global/presentation/GlobalExceptionHandler`가 `ErrorResponse`로 변환한다
- 도메인별 에러 코드는 `global/domain/exception/ErrorCode`에 추가한다. domain이 Spring Web에 의존하지 않도록 `HttpStatus`가 아닌 `int status`를 쓴다
- 상태 흐름 규칙 위반(이미 결제된 주문 결제, 결제 후 취소 등)은 **409로 통일**

## 반드시 지킬 규칙 (과제 요구사항)

- 모든 엔티티는 `@MappedSuperclass` BaseEntity를 상속해 생성·수정 시각을 JPA Auditing으로 기록 (`@EnableJpaAuditing` 필요)
- 테이블 이름에 `user`, `order` 사용 금지 (PostgreSQL 예약어) → `@Table(name = "p_user")` 등으로 변경
- enum은 `@Enumerated(EnumType.STRING)`
- 필수 컬럼 `nullable = false`, 로그인 아이디 `unique = true` — 서비스 검사와 별개로 DB 제약도 건다
- 메뉴 삭제는 Soft Delete. 삭제된 메뉴는 목록에서 제외, 단건 조회·수정·삭제·주문에서는 404. 기존 주문 기록은 유지
- 요청·응답은 DTO로만. Entity를 그대로 응답하지 않으며 응답에 비밀번호를 담지 않는다
- 주문 총액(메뉴 가격 × 수량)과 결제 금액은 **서버가 계산** — 요청에서 금액을 받지 않음
- 메뉴 주인·주문자 등 "누가"는 요청 본문이 아니라 **JWT에서** 꺼낸다
- 역할별 주문 목록·아이디 중복 확인은 Spring Data JPA Query Methods로 구현
- 비밀번호는 BCrypt. JWT에는 아이디·역할·만료시간만 (HS256 비밀키는 32바이트 이상)

## Spring Security 함정 (발제 3-5)

- `/error`를 `permitAll()` 하지 않으면 400/404/409/500 등이 모두 빈 403으로 바뀐다
- JWT API이므로 `csrf.disable()` — 안 하면 POST/PATCH/DELETE가 전부 403
- 토큰 없는 요청은 기본적으로 403 (401 구분은 도전 기능)
- `ddl-auto: update`는 기존 CHECK 제약을 갱신하지 않으므로, enum 값을 추가하면 한 번 `create`로 재생성해야 한다

## 설정 · 비밀값

`src/main/resources/application.yaml`은 git에 추적되며 레포는 Public으로 제출된다. DB 비밀번호·JWT 비밀키는 하드코딩하지 말고 환경변수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` — README 기준)로 주입한다. `application.properties`와 yml을 함께 두지 않는다 (properties가 우선해 yml 변경이 무시됨).

## Git 워크플로

GitHub Flow. `main`에서 `<type>/<desc>` 브랜치를 따고 PR → Squash merge. 한 번에 한 브랜치만 진행하며 순서는 `docs/design` → `chore/init-setup` → `feat/auth` → `feat/menu` → `feat/order` → `feat/payment` → `feat/challenge-*`. 커밋 메시지는 `feat: 메뉴 등록 API 구현` 형식 (type: feat·fix·refactor·docs·chore·test). 상세는 README의 "브랜치 전략" 참고.
