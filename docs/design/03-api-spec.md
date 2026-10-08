# 03. API 명세서

## 변경 이력

| 버전 | 날짜 | 변경 내용 | 관련 요구사항 |
|---|---|---|---|
| v1.0 | 2026-10-07 | 최초 작성 (필수 API 13개, 도전 API 2개, 공통 규칙, 에러 응답) | 01 v1.4, 02 v1.3, 04 v1.3 |
| v1.3 | 2026-10-08 | 4.4 에러 응답 예시(409)의 메시지를 실제 `ErrorCode.INVALID_ORDER_STATUS` 문구와 맞춤 | 구현 대조 |
| v1.2 | 2026-10-08 | 회원가입 역할 값 오류(`ADMIN` 등)도 `fieldErrors`에 `role`을 담아 응답 (역할을 문자열로 받아 `@Pattern` 검증, 04 D-32) | 04 v1.7 |
| v1.1 | 2026-10-07 | 7장 DTO 목록에 위치 표시: 요청 DTO는 presentation, Facade 입력(Command)·응답 DTO는 application (API 모양 변화 없음) | 04 v1.5 D-30, v1.6 D-31 |

<br>

## 1. 요구사항 요약 (근거)

- [01 요구사항 정의서](01-requirements.md): 기능별 규칙과 실패 조건(6장), 검증 순서 D-01, 상태 변경 API D-02, JWT D-03, 권한 매트릭스(7.4)
- [02 도메인 설계](02-domain.md): 컬럼 길이와 타입, 판매 상태, 스냅샷
- [04 클래스 다이어그램](04-class-diagram.md): 에러 코드(6장)
- 발제 자료 4-3 (RESTful URL 규칙, 상태 코드, 명세서 형식), 5-1 (테스트 시나리오)

<br>

## 2. 쉬운 설명 (한눈에)

> 이 문서는 **프론트엔드 개발자가 이것만 보고 화면을 만들 수 있게** 하는 사용 설명서입니다.
>
> - 주소(URL)는 **명사**(`/menus`, `/orders`)로, 하고 싶은 일은 **HTTP 메서드**(조회 GET, 생성 POST, 수정 PUT·PATCH, 삭제 DELETE)로 표현합니다.
> - 로그인하면 받는 **출입증(토큰)**을 이후 요청 헤더에 붙여 보냅니다.
> - 실패하면 항상 같은 모양의 에러 응답이 옵니다. `code`만 보면 무슨 문제인지 알 수 있습니다.

<br>

## 3. 설계 결정

| ID | 결정점 | 선택 | 이유 | 포기한 것 |
|---|---|---|---|---|
| D-18 | 회원가입·로그인 URL | `POST /api/auth/signup`, `POST /api/auth/login` | 인증 기능을 한 경로에 모아 Security에서 `/api/auth/**`를 한 줄로 허용한다 | 회원가입을 `POST /api/users`(회원 자원 생성)로 보는 REST 정석 |
| D-19 | 성공 응답 코드 | 상태 변경(취소·수락·배달완료)은 **200 + 변경된 주문**, 메뉴 삭제는 **204** | 상태 변경 결과를 응답에서 바로 확인할 수 있다(시나리오 #28·#32). 삭제는 돌려줄 내용이 없다 | 모든 변경을 204로 통일하는 간결함 |
| D-20 | 주문 응답의 주문자 정보 | **`customerUsername` 포함** | 사장님이 주문자를 알 수 있다. 목록 조회는 `@EntityGraph`로 회원을 함께 조회해 N+1을 막는다 | 추가 조회 없는 단순함 |
| D-21 | 비밀번호 길이 | **8~20자** | BCrypt는 72바이트까지만 처리하고, 초과하면 Spring Security가 예외를 던진다(500). 20자는 한글(3바이트)로 채워도 60바이트라 안전하다 | 긴 비밀번호(패스프레이즈) 허용 |
| D-22 | 결제 경로 | `POST /api/orders/{orderId}/payments` | 결제는 주문에 딸린 자원이다 (발제 4-3 "딸린 자원은 계층으로") | 결제를 독립 자원(`/api/payments`)으로 두는 방식 |
| D-23 | 메뉴 수정 메서드 | `PUT` (이름·가격·설명 전체 교체) | 발제가 "이름·가격·설명을 수정, 검증은 등록과 같다"고 해 전체 교체가 자연스럽다 | 일부 필드만 보내는 `PATCH`의 편의 |
| D-24 | 페이지 응답 형식 | 직접 정의한 `PageResponse` | Spring의 `Page`를 그대로 직렬화하면 JSON 구조가 내부 구현에 묶이고 불안정하다 | 별도 DTO 없이 `Page`를 반환하는 간결함 |
| D-25 | 성공 응답 껍데기 | **없음** (데이터를 그대로 반환) | HTTP 상태 코드가 성공·실패를 표현하고, 에러는 이미 `ErrorResponse`로 통일되어 있다 | `{ success, data }` 공통 껍데기의 일관성 |

<br>

## 4. 공통 규칙

### 4.1 요청

| 항목 | 규칙 |
|---|---|
| 기본 경로 | `http://localhost:8080/api` |
| 본문 형식 | `Content-Type: application/json` |
| 인증 | `Authorization: Bearer {accessToken}` — 로그인이 필요한 API만 |
| 경로 변수 | 숫자 ID. 숫자가 아니면 400 |

### 4.2 응답

| 항목 | 규칙 |
|---|---|
| 본문 형식 | JSON, 껍데기 없이 데이터 그대로 (D-25) |
| 시각 | ISO-8601, 초 단위 (`"2026-10-07T14:05:00"`) |
| 금액 | 원 단위 정수 (`7000`) |
| enum | 문자열 (`"ORDERED"`, `"CARD"`) |
| 비밀번호 | **어떤 응답에도 포함하지 않는다** |

### 4.3 상태 코드

| 코드 | 의미 | 이 API에서 쓰는 경우 |
|---|---|---|
| 200 | 성공 | 조회, 수정, 로그인, 주문 상태 변경 |
| 201 | 생성됨 | 회원가입, 메뉴 등록, 주문 생성, 결제 |
| 204 | 성공 (본문 없음) | 메뉴 삭제 |
| 400 | 잘못된 요청 | 입력 검증 실패, JSON 형식 오류, enum에 없는 값, 경로 변수 타입 오류 |
| 401 | 인증 실패 | 로그인 실패, 토큰 없음·만료·변조 (C-04 적용 후) |
| 403 | 권한 없음 | 역할이 맞지 않음, 남의 메뉴·주문 |
| 404 | 없음 | 없는 메뉴·주문, 삭제된 메뉴 |
| 409 | 충돌 | 아이디 중복, 허용되지 않는 주문 상태 변경, 품절 메뉴 주문, 동시 수정 충돌 |

여러 규칙을 동시에 어기면 [01 D-01 검증 순서](01-requirements.md#71-검증-순서-d-01)대로 **먼저 걸린 것 하나**를 응답합니다: 401 → 403(역할) → 400 → 404 → 403(소유) → 409

> **C-04 적용 전 주의:** 토큰이 없거나 잘못된 요청은 Spring Security 기본 동작으로 **본문 없는 403**이 응답됩니다(발제 3-5 ③). C-04를 적용하면 401·403이 구분되고 본문도 아래 에러 형식을 따릅니다.

### 4.4 에러 응답

모든 에러는 같은 형식입니다 (`ErrorResponse`).

| 필드 | 타입 | 설명 |
|---|---|---|
| `status` | Number | HTTP 상태 코드 |
| `code` | String | 에러 코드 (아래 표) |
| `message` | String | 사람이 읽는 설명 |
| `fieldErrors` | Array | 입력 검증 실패일 때만 값이 있음. 그 외에는 빈 배열 |
| `fieldErrors[].field` | String | 잘못된 필드 이름 |
| `fieldErrors[].reason` | String | 실패 이유 |

**예시 — 입력 검증 실패 (400)**

```json
{
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": [
    { "field": "price", "reason": "가격은 1원 이상이어야 합니다." }
  ]
}
```

**예시 — 상태 규칙 위반 (409)**

```json
{
  "status": 409,
  "code": "INVALID_ORDER_STATUS",
  "message": "현재 주문 상태에서는 할 수 없는 요청입니다.",
  "fieldErrors": []
}
```

**에러 코드 목록**

| code | 상태 | 상황 |
|---|---|---|
| `INVALID_INPUT` | 400 | 입력 검증 실패, JSON 형식 오류, enum에 없는 값, 경로 변수 타입 오류 |
| `UNAUTHORIZED` | 401 | 토큰 없음·만료·변조 (C-04 적용 후) |
| `INVALID_CREDENTIALS` | 401 | 로그인 실패 (아이디 없음, 비밀번호 불일치, 탈퇴 회원 — 구분하지 않음) |
| `ACCESS_DENIED` | 403 | 역할이 맞지 않음 (C-04 적용 후) |
| `MENU_ACCESS_DENIED` | 403 | 다른 사장님의 메뉴 |
| `ORDER_ACCESS_DENIED` | 403 | 다른 손님의 주문, 다른 사장님 메뉴의 주문 |
| `MENU_NOT_FOUND` | 404 | 없거나 삭제된 메뉴 |
| `ORDER_NOT_FOUND` | 404 | 없는 주문 |
| `RESOURCE_NOT_FOUND` | 404 | 없는 URL |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| `DUPLICATE_USERNAME` | 409 | 이미 있는 아이디 (탈퇴 회원 포함) |
| `INVALID_ORDER_STATUS` | 409 | 허용되지 않는 주문 상태 변경 |
| `MENU_SOLD_OUT` | 409 | 품절 메뉴 주문 |
| `CONCURRENT_MODIFICATION` | 409 | 같은 주문을 동시에 변경 (낙관적 락 충돌) |
| `INTERNAL_SERVER_ERROR` | 500 | 예상하지 못한 서버 오류 |

### 4.5 페이지 응답 (D-24)

| 필드 | 타입 | 설명 |
|---|---|---|
| `content` | Array | 현재 페이지의 항목 |
| `page` | Number | 현재 페이지 번호 (0부터) |
| `size` | Number | 페이지 크기 |
| `totalElements` | Number | 전체 항목 수 |
| `totalPages` | Number | 전체 페이지 수 |
| `hasNext` | Boolean | 다음 페이지 존재 여부 |

<br>

## 5. API 목록

| # | ID | 기능 | Method | URL | 권한 | 성공 |
|---|---|---|---|---|---|---|
| 1 | F-01 | 회원가입 | POST | `/api/auth/signup` | 누구나 | 201 |
| 2 | F-02 | 로그인 | POST | `/api/auth/login` | 누구나 | 200 |
| 3 | F-03 | 메뉴 등록 | POST | `/api/menus` | OWNER | 201 |
| 4 | F-04 | 메뉴 목록 조회 | GET | `/api/menus` | 누구나 | 200 |
| 5 | F-05 | 메뉴 단건 조회 | GET | `/api/menus/{menuId}` | 누구나 | 200 |
| 6 | F-06 | 메뉴 수정 | PUT | `/api/menus/{menuId}` | OWNER · 본인 메뉴 | 200 |
| 7 | F-07 | 메뉴 삭제 | DELETE | `/api/menus/{menuId}` | OWNER · 본인 메뉴 | 204 |
| 8 | F-08 | 주문 생성 | POST | `/api/orders` | CUSTOMER | 201 |
| 9 | F-09 | 주문 목록 조회 | GET | `/api/orders` | 로그인 사용자 | 200 |
| 10 | F-10 | 주문 취소 | PATCH | `/api/orders/{orderId}/cancel` | CUSTOMER · 본인 주문 | 200 |
| 11 | F-11 | 주문 수락 | PATCH | `/api/orders/{orderId}/accept` | OWNER · 본인 메뉴 주문 | 200 |
| 12 | F-11 | 주문 배달완료 | PATCH | `/api/orders/{orderId}/complete` | OWNER · 본인 메뉴 주문 | 200 |
| 13 | F-12 | 결제 | POST | `/api/orders/{orderId}/payments` | CUSTOMER · 본인 주문 | 201 |
| 14 | C-01 | 주문 단건 조회 | GET | `/api/orders/{orderId}` | 로그인 사용자 · 본인 관련 주문 | 200 |
| 15 | C-02 | 결제 내역 조회 | GET | `/api/orders/{orderId}/payments` | CUSTOMER · 본인 주문 | 200 |

### 5.1 Security URL 규칙

[01 권한 매트릭스](01-requirements.md#74-권한-매트릭스)의 ❌(역할만으로 거절)를 그대로 옮긴 것입니다. 🔒(본인 것만)는 도메인 서비스에서 검사합니다. **위에서부터 순서대로** 적용하므로, 구체적인 규칙을 일반 규칙(`/api/orders/*`)보다 먼저 둡니다.

| Method | URL | 규칙 |
|---|---|---|
| ALL | `/error` | 허용 (발제 3-5 ②) |
| POST | `/api/auth/**` | 허용 |
| GET | `/api/menus`, `/api/menus/*` | 허용 |
| POST · PUT · DELETE | `/api/menus/**` | `OWNER` |
| POST | `/api/orders` | `CUSTOMER` |
| PATCH | `/api/orders/*/cancel` | `CUSTOMER` |
| PATCH | `/api/orders/*/accept`, `/api/orders/*/complete` | `OWNER` |
| POST · GET | `/api/orders/*/payments` | `CUSTOMER` |
| GET | `/api/orders`, `/api/orders/*` | 로그인 |
| 그 외 | 모든 요청 | 로그인 |

<br>

## 6. API 상세

### 👤 회원

#### 1. 회원가입 (F-01)

| 항목 | 내용 |
|---|---|
| Method · URL | `POST /api/auth/signup` |
| 권한 | 누구나 |
| 성공 | `201 Created` |
| 실패 | `400 INVALID_INPUT` 값 누락·길이 위반·역할 값 오류 · `409 DUPLICATE_USERNAME` 이미 있는 아이디 |

**요청 필드**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `username` | String | O | 4~20자 | 로그인 아이디 |
| `password` | String | O | 8~20자 (D-21) | 비밀번호 |
| `role` | String | O | `CUSTOMER` 또는 `OWNER` (아니면 400, `fieldErrors`에 `role`) | 역할 |

**요청 예시**

```http
POST /api/auth/signup
Content-Type: application/json

{
  "username": "owner1",
  "password": "password123",
  "role": "OWNER"
}
```

**응답 예시** — `201 Created`

```json
{
  "userId": 1,
  "username": "owner1",
  "role": "OWNER",
  "createdAt": "2026-10-07T14:00:00"
}
```

---

#### 2. 로그인 (F-02)

| 항목 | 내용 |
|---|---|
| Method · URL | `POST /api/auth/login` |
| 권한 | 누구나 |
| 성공 | `200 OK` |
| 실패 | `400 INVALID_INPUT` 값 누락 · `401 INVALID_CREDENTIALS` 아이디 없음, 비밀번호 불일치, 탈퇴 회원 |

**요청 필드**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `username` | String | O | 로그인 아이디 |
| `password` | String | O | 비밀번호 |

**요청 예시**

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "owner1",
  "password": "password123"
}
```

**응답 예시** — `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

| 필드 | 설명 |
|---|---|
| `accessToken` | JWT. 클레임: `sub`(회원 PK), `username`, `role`, `exp` |
| `tokenType` | 항상 `"Bearer"` |
| `expiresIn` | 만료까지 남은 초 (3600 = 1시간) |

---

### 🍜 메뉴

**메뉴 응답 (`MenuResponse`)** — 메뉴 API 공통

| 필드 | 타입 | 설명 |
|---|---|---|
| `menuId` | Number | 메뉴 ID |
| `ownerId` | Number | 사장님 회원 ID |
| `name` | String | 이름 |
| `price` | Number | 가격 |
| `description` | String \| null | 설명 |
| `status` | String | 판매 상태 `ON_SALE`, `SOLD_OUT` |
| `createdAt` | String | 등록 시각 |
| `updatedAt` | String | 수정 시각 |

#### 3. 메뉴 등록 (F-03)

| 항목 | 내용 |
|---|---|
| Method · URL | `POST /api/menus` |
| 권한 | OWNER (메뉴 주인 = 토큰의 회원) |
| 성공 | `201 Created` |
| 실패 | `401` 토큰 없음 · `403` CUSTOMER · `400 INVALID_INPUT` 이름 없음, 가격 1원 미만, 길이 초과 |

**요청 필드**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `name` | String | O | 공백 불가, 최대 100자 | 메뉴 이름 |
| `price` | Number | O | 1 이상 | 가격 |
| `description` | String | X | 최대 500자 | 설명 |

**요청 예시** (시나리오 #11)

```http
POST /api/menus
Authorization: Bearer {owner1 토큰}
Content-Type: application/json

{
  "name": "김밥",
  "price": 3000,
  "description": "참기름 향 가득한 기본 김밥"
}
```

**응답 예시** — `201 Created`

```json
{
  "menuId": 1,
  "ownerId": 1,
  "name": "김밥",
  "price": 3000,
  "description": "참기름 향 가득한 기본 김밥",
  "status": "ON_SALE",
  "createdAt": "2026-10-07T14:05:00",
  "updatedAt": "2026-10-07T14:05:00"
}
```

---

#### 4. 메뉴 목록 조회 (F-04, C-03)

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/menus?page={page}&size={size}` |
| 권한 | 누구나 (토큰 불필요) |
| 성공 | `200 OK` — `PageResponse<MenuResponse>` |
| 규칙 | 삭제된 메뉴 제외, 품절 메뉴 포함, **최신 등록순 고정** |

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | Number | X | 0 | 페이지 번호 (0부터) |
| `size` | Number | X | 10 | 페이지 크기 (최대 50) |

> 음수 `page`는 0으로, 50을 넘는 `size`는 50으로 보정됩니다 (Spring Data 기본 동작, `max-page-size: 50`). `sort` 파라미터는 받지 않습니다.

**응답 예시** — `200 OK`

```json
{
  "content": [
    {
      "menuId": 1,
      "ownerId": 1,
      "name": "김밥",
      "price": 3000,
      "description": "참기름 향 가득한 기본 김밥",
      "status": "ON_SALE",
      "createdAt": "2026-10-07T14:05:00",
      "updatedAt": "2026-10-07T14:05:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

---

#### 5. 메뉴 단건 조회 (F-05)

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/menus/{menuId}` |
| 권한 | 누구나 (토큰 불필요) |
| 성공 | `200 OK` — `MenuResponse` |
| 실패 | `400 INVALID_INPUT` ID가 숫자가 아님 · `404 MENU_NOT_FOUND` 없거나 삭제된 메뉴 |

---

#### 6. 메뉴 수정 (F-06)

| 항목 | 내용 |
|---|---|
| Method · URL | `PUT /api/menus/{menuId}` |
| 권한 | OWNER · 본인 메뉴 |
| 성공 | `200 OK` — 수정된 `MenuResponse` (`updatedAt` 갱신) |
| 실패 | `401` · `403` CUSTOMER · `400 INVALID_INPUT` (등록과 같은 검증) · `404 MENU_NOT_FOUND` 없거나 삭제된 메뉴 · `403 MENU_ACCESS_DENIED` 다른 사장님의 메뉴 |

**요청 필드** — 메뉴 등록과 같습니다. **세 필드를 모두 보내며, 보낸 값으로 전체 교체**합니다(D-23). `description`을 빼면 설명이 비워집니다.

**요청 예시** (시나리오 #16)

```http
PUT /api/menus/1
Authorization: Bearer {owner1 토큰}
Content-Type: application/json

{
  "name": "김밥",
  "price": 3500,
  "description": "참기름 향 가득한 기본 김밥"
}
```

---

#### 7. 메뉴 삭제 (F-07)

| 항목 | 내용 |
|---|---|
| Method · URL | `DELETE /api/menus/{menuId}` |
| 권한 | OWNER · 본인 메뉴 |
| 성공 | `204 No Content` (Soft Delete — 행은 남고 `deleted_at`만 기록) |
| 실패 | `401` · `403` CUSTOMER · `404 MENU_NOT_FOUND` 없거나 이미 삭제된 메뉴 · `403 MENU_ACCESS_DENIED` 다른 사장님의 메뉴 |

---

### 🧾 주문

**주문 응답 (`OrderResponse`)** — 주문 API 공통

| 필드 | 타입 | 설명 |
|---|---|---|
| `orderId` | Number | 주문 ID |
| `customerUsername` | String | 주문자 아이디 (D-20) |
| `menuId` | Number | 메뉴 ID (삭제된 메뉴여도 값 유지) |
| `menuName` | String | 주문 당시 메뉴 이름 (스냅샷) |
| `unitPrice` | Number | 주문 당시 단가 (스냅샷) |
| `quantity` | Number | 수량 |
| `totalPrice` | Number | 총액 |
| `deliveryAddress` | String | 배송 주소 |
| `status` | String | `ORDERED`, `PAID`, `ACCEPTED`, `COMPLETED`, `CANCELED` |
| `createdAt` | String | 주문 시각 |
| `updatedAt` | String | 마지막 상태 변경 시각 |

#### 8. 주문 생성 (F-08)

| 항목 | 내용 |
|---|---|
| Method · URL | `POST /api/orders` |
| 권한 | CUSTOMER (주문자 = 토큰의 회원) |
| 성공 | `201 Created` — `OrderResponse` (상태 `ORDERED`) |
| 실패 | `401` · `403` OWNER · `400 INVALID_INPUT` 수량 1 미만, 배송 주소 없음, 길이 초과 · `404 MENU_NOT_FOUND` 없거나 삭제된 메뉴 · `409 MENU_SOLD_OUT` 품절 메뉴 |

**요청 필드** — 금액은 받지 않습니다. 총액은 서버가 계산합니다.

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `menuId` | Number | O | — | 주문할 메뉴 |
| `quantity` | Number | O | 1 이상 | 수량 |
| `deliveryAddress` | String | O | 공백 불가, 최대 255자 | 배송 주소 |

**요청 예시** (시나리오 #18)

```http
POST /api/orders
Authorization: Bearer {cust1 토큰}
Content-Type: application/json

{
  "menuId": 1,
  "quantity": 2,
  "deliveryAddress": "서울시 강남구 테헤란로 1"
}
```

**응답 예시** — `201 Created`

```json
{
  "orderId": 1,
  "customerUsername": "cust1",
  "menuId": 1,
  "menuName": "김밥",
  "unitPrice": 3500,
  "quantity": 2,
  "totalPrice": 7000,
  "deliveryAddress": "서울시 강남구 테헤란로 1",
  "status": "ORDERED",
  "createdAt": "2026-10-07T14:10:00",
  "updatedAt": "2026-10-07T14:10:00"
}
```

---

#### 9. 주문 목록 조회 (F-09)

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/orders` |
| 권한 | 로그인 사용자 |
| 성공 | `200 OK` — `OrderResponse` 배열, **최신 주문순** |
| 규칙 | CUSTOMER: 내가 한 주문 / OWNER: 내 메뉴에 들어온 주문 (삭제된 메뉴의 주문 포함). 모든 상태 포함 |
| 실패 | `401` |

**응답 예시** — `200 OK` (주문이 없으면 `[]`)

```json
[
  {
    "orderId": 1,
    "customerUsername": "cust1",
    "menuId": 1,
    "menuName": "김밥",
    "unitPrice": 3500,
    "quantity": 2,
    "totalPrice": 7000,
    "deliveryAddress": "서울시 강남구 테헤란로 1",
    "status": "ORDERED",
    "createdAt": "2026-10-07T14:10:00",
    "updatedAt": "2026-10-07T14:10:00"
  }
]
```

> 주문 목록은 페이징하지 않습니다 (C-03은 메뉴 목록만 해당).

---

#### 10. 주문 취소 (F-10)

| 항목 | 내용 |
|---|---|
| Method · URL | `PATCH /api/orders/{orderId}/cancel` |
| 권한 | CUSTOMER · 본인 주문 |
| 요청 본문 | 없음 |
| 성공 | `200 OK` — 변경된 `OrderResponse` (상태 `CANCELED`) |
| 실패 | `401` · `403` OWNER · `404 ORDER_NOT_FOUND` · `403 ORDER_ACCESS_DENIED` 다른 손님의 주문 · `409 INVALID_ORDER_STATUS` `ORDERED`가 아닌 주문 · `409 CONCURRENT_MODIFICATION` |

---

#### 11. 주문 수락 (F-11)

| 항목 | 내용 |
|---|---|
| Method · URL | `PATCH /api/orders/{orderId}/accept` |
| 권한 | OWNER · 본인 메뉴의 주문 |
| 요청 본문 | 없음 |
| 성공 | `200 OK` — 변경된 `OrderResponse` (상태 `ACCEPTED`) |
| 실패 | `401` · `403` CUSTOMER · `404 ORDER_NOT_FOUND` · `403 ORDER_ACCESS_DENIED` 다른 사장님 메뉴의 주문 · `409 INVALID_ORDER_STATUS` `PAID`가 아닌 주문 · `409 CONCURRENT_MODIFICATION` |

---

#### 12. 주문 배달완료 (F-11)

| 항목 | 내용 |
|---|---|
| Method · URL | `PATCH /api/orders/{orderId}/complete` |
| 권한 | OWNER · 본인 메뉴의 주문 |
| 요청 본문 | 없음 |
| 성공 | `200 OK` — 변경된 `OrderResponse` (상태 `COMPLETED`) |
| 실패 | `401` · `403` CUSTOMER · `404 ORDER_NOT_FOUND` · `403 ORDER_ACCESS_DENIED` · `409 INVALID_ORDER_STATUS` `ACCEPTED`가 아닌 주문 · `409 CONCURRENT_MODIFICATION` |

---

### 💳 결제

**결제 응답 (`PaymentResponse`)**

| 필드 | 타입 | 설명 |
|---|---|---|
| `paymentId` | Number | 결제 ID |
| `orderId` | Number | 주문 ID |
| `amount` | Number | 결제 금액 (= 주문 총액) |
| `method` | String | `CARD` |
| `status` | String | 결제 상태 `COMPLETED` |
| `orderStatus` | String | 결제 후 주문 상태 (시나리오 #24 확인용) |
| `createdAt` | String | 결제 시각 |

#### 13. 결제 (F-12)

| 항목 | 내용 |
|---|---|
| Method · URL | `POST /api/orders/{orderId}/payments` |
| 권한 | CUSTOMER · 본인 주문 |
| 성공 | `201 Created` — `PaymentResponse` |
| 실패 | `401` · `403` OWNER · `400 INVALID_INPUT` 결제 수단 누락, `CARD`가 아닌 값 · `404 ORDER_NOT_FOUND` · `403 ORDER_ACCESS_DENIED` 다른 손님의 주문 · `409 INVALID_ORDER_STATUS` `ORDERED`가 아닌 주문 · `409 CONCURRENT_MODIFICATION` 동시 결제 |

**요청 필드** — 금액은 받지 않습니다.

| 필드 | 타입 | 필수 | 검증 | 설명 |
|---|---|---|---|---|
| `method` | String | O | `CARD`만 | 결제 수단 |

**요청 예시** (시나리오 #24)

```http
POST /api/orders/1/payments
Authorization: Bearer {cust1 토큰}
Content-Type: application/json

{
  "method": "CARD"
}
```

**응답 예시** — `201 Created`

```json
{
  "paymentId": 1,
  "orderId": 1,
  "amount": 7000,
  "method": "CARD",
  "status": "COMPLETED",
  "orderStatus": "PAID",
  "createdAt": "2026-10-07T14:15:00"
}
```

---

### ⏭️ 도전 기능

#### 14. 주문 단건 조회 (C-01)

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/orders/{orderId}` |
| 권한 | CUSTOMER: 본인 주문 / OWNER: 본인 메뉴의 주문 |
| 성공 | `200 OK` — `OrderResponse` |
| 실패 | `401` · `404 ORDER_NOT_FOUND` · `403 ORDER_ACCESS_DENIED` |

#### 15. 결제 내역 조회 (C-02)

| 항목 | 내용 |
|---|---|
| Method · URL | `GET /api/orders/{orderId}/payments` |
| 권한 | CUSTOMER · 본인 주문 |
| 성공 | `200 OK` — `PaymentResponse` 배열 (결제 기록이 없으면 `[]`) |
| 실패 | `401` · `403` OWNER · `404 ORDER_NOT_FOUND` · `403 ORDER_ACCESS_DENIED` |

> 이 API의 `orderStatus`는 조회 시점의 주문 상태입니다.

<br>

## 7. DTO 목록

[04 클래스 다이어그램](04-class-diagram.md)의 DTO 이름입니다. 요청 DTO는 presentation에 두고 `toCommand()`로 application의 입력(Command)으로 바꿉니다. 응답 DTO는 application이 만들어 presentation이 그대로 응답합니다(04 D-30).

| 도메인 | 요청 DTO (presentation) | Facade 입력 (application) | 응답 DTO (application) |
|---|---|---|---|
| user | `SignupRequest`, `LoginRequest` | `SignupCommand`, `LoginCommand` | `UserResponse`, `LoginResponse` |
| menu | `MenuRequest` (등록·수정 공통) | `MenuCommand` | `MenuResponse` |
| order | `OrderCreateRequest` | `OrderCreateCommand` | `OrderResponse` |
| payment | `PaymentRequest` | `PaymentCommand` | `PaymentResponse` |
| global | — | — | `PageResponse<T>` (application), `ErrorResponse` (presentation) |

<br>

## 8. 잠재 리스크

| 리스크 | 상황 | 대응 |
|---|---|---|
| C-04 전 에러 형식 불일치 | 토큰 없음·역할 불일치는 Security가 막아 `ErrorResponse`가 아닌 본문 없는 403이 나감 | C-04에서 `AuthenticationEntryPoint`·`AccessDeniedHandler`로 통일. 그 전까지 4.3의 주의 문구로 안내 |
| 주문 목록 N+1 | `customerUsername`을 위해 주문마다 회원을 조회할 수 있음 | 목록 Query Method에 `@EntityGraph(attributePaths = "customer")`. Repository 테스트에서 쿼리 수 확인 |
| 주문 목록 크기 | 주문이 많아지면 페이징 없이 전부 반환 | 과제 범위에서는 허용. 필요하면 메뉴 목록과 같은 `PageResponse` 적용 |
| 메뉴 수정 시 설명 유실 | `PUT`이라 `description`을 빠뜨리면 설명이 지워짐 | 의도된 동작(D-23). 명세에 명시 |
| 비밀번호 길이 | 20자 초과 입력은 400 | BCrypt 72바이트 예외(500)를 막기 위한 의도된 제한(D-21) |
