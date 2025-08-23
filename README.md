# **API 명세서 (Original Version with JSON examples)**

## 1. 개요

본 문서는 공급망 데이터 분석 시스템의 백엔드 API 명세를 정의합니다. 모든 API는 Base URL (`http://localhost:8080`)을 기준으로 합니다.

### 1.1. 인증 (Authentication)

*   인증이 필요한 모든 API는 요청 헤더에 `Authorization: Bearer <JWT>` 토큰을 포함해야 합니다.
*   토큰은 `POST /api/public/login` API를 통해 발급받을 수 있습니다. (※ `login` API는 JWT 필터로 처리되어 명세에는 없음)

---

## 2. Public API (인증 불필요)

### `MemberJoinController`

#### **2.1. 회원 가입**
*   `POST /api/public/join`
*   **설명:** 새로운 사용자를 시스템에 등록합니다.
*   **Request Body (`application/json`):**
    ```json
    {
      "userId": "newUser",
      "userName": "홍길동",
      "password": "password123",
      "email": "new@user.com",
      "phone": "010-1234-5678",
      "locationId": 1001,
      "role": "ROLE_MANAGER"
    }
    ```
*   **Success Response (200 OK):**
    *   `Body`: 없음

#### **2.2. 아이디 중복 확인**
*   `POST /api/public/join/idsearch`
*   **설명:** 회원가입 전, 사용자 아이디의 중복 여부를 확인합니다.
*   **Request Body (`application/json`):**
    ```json
    {
      "userId": "newUser",
      "password": null
    }
    ```
*   **Success Response (200 OK):**
    *   **Body (`application/json`):** `true` (사용 중) 또는 `false` (사용 가능)

---

## 3. Manager API (Manager, Admin 권한)

### `MemberSettingController`

#### **3.1. 내 정보 조회**
*   `GET /api/manager/setting/user`
*   **설명:** 현재 로그인한 사용자의 정보를 조회합니다.
*   **Success Response (200 OK):**
    *   **Body (`application/json`):**
        ```json
        {
          "userName": "홍길동",
          "email": "current@user.com",
          "status": "active"
        }
        ```

#### **3.2. 비밀번호 변경**
*   `POST /api/manager/setting/password`
*   **설명:** 현재 로그인한 사용자의 비밀번호를 변경합니다.
*   **Request Body (`application/json`):**
    ```json
    {
      "currentPassword": "oldPassword123",
      "newPassword": "newPassword456!"
    }
    ```
*   **Success Response (200 OK or 409 Conflict):**
    *   **Body (`text/plain`):** `변경 성공`

#### **3.3. 내 정보 수정**
*   `PATCH /api/manager/setting/info`
*   **설명:** 현재 로그인한 사용자의 정보를 수정합니다.
*   **Request Body (`application/json`):**
    ```json
    {
      "userName": "김철수",
      "email": "newemail@user.com",
      "status": "active"
    }
    ```
*   **Success Response (200 OK):**
    *   **Body (`application/json`):**
        ```json
        {
          "userName": "김철수",
          "userId": "currentUser",
          "status": "active",
          "email": "newemail@user.com"
        }
        ```

### `CsvController`

#### **3.4. CSV 파일 업로드**
*   `POST /api/manager/upload`
*   **설명:** CSV 파일을 업로드하고 비동기 분석 파이프라인을 시작합니다.
*   **Request:** `multipart/form-data` (key: `file`, value: `(CSV 파일)`)
*   **Success Response (200 OK):**
    *   **Body (`application/json`):**
        ```json
        {
          "message": "업로드 시작됨. 파일 ID: 101. 진행상황은 실시간으로 알림됩니다."
        }
        ```

#### **3.5. 업로드 파일 목록 조회**
*   `GET /api/manager/upload/filelist`
*   **설명:** 업로드된 CSV 파일 목록을 커서 기반 페이징으로 조회합니다.
*   **Query Parameters:** `cursor` (Long), `size` (int), `search` (String)
*   **Success Response (200 OK):**
    *   **Body (`application/json`):**
        ```json
        {
          "data": [
            {
              "fileId": 101,
              "fileName": "sample_data_01.csv",
              "userId": "manager1",
              "fileSize": 102400,
              "createdAt": "2024-08-21T10:00:00"
            },
            {
              "fileId": 100,
              "fileName": "sample_data_00.csv",
              "userId": "manager1",
              "fileSize": 51200,
              "createdAt": "2024-08-20T15:30:00"
            }
          ],
          "nextCursor": 100
        }
        ```

#### **3.6. CSV 파일 다운로드**
*   `GET /api/manager/download/{fileId}`
*   **설명:** 원본 CSV 파일을 다운로드합니다.
*   **Success Response (200 OK):**
    *   **Body:** (CSV 파일 바이너리 데이터)

#### **3.7. AI 분석 재요청**
*   `POST /api/manager/resend/{fileId}`
*   **설명:** 특정 파일에 대해 AI 분석을 다시 요청합니다.
*   **Success Response (200 OK):**
    *   **Body (`text/plain`):** `AI 모듈로 재전송 성공!`

### `DashboardController` & `StatisticsController`

| Method | URL                     | 설명                                | Response (200 OK) JSON Example                                                                                                                                                                             |
| :----- | :---------------------- | :---------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/api/manager/nodes`    | 노드 목록 조회                        | `[{"hubType": "Factory", "scanLocation": "부산공장", "businessStep": "생산", "coord": [129.0, 35.0]}, ...]`                                                                                                         |
| `GET`  | `/api/manager/anomalies`| 이상 탐지 목록 조회 (페이징)          | `{"data": [...AnalyzedTripDTO...], "nextCursor": 55, "pageSize": 50, "hasNext": true}`                                                                                                                           |
| `GET`  | `/api/manager/trips`    | 전체 이동 경로 목록 조회 (페이징)     | `{"data": [...AnalyzedTripDTO...], "nextCursor": 123, "pageSize": 50, "hasNext": true}`                                                                                                                          |
| `GET`  | `/api/manager/trips/filter`| 필터링 데이터 목록 조회               | `{"scanLocations": ["부산공장", "서울허브"], "eventTimeRange": ["2024-08-01T00:00:00", "2024-08-21T23:59:59"], ...}`                                                                                             |
| `GET`  | `/api/manager/trips/from`| 출발지 기준 도착지 목록 조회          | `{"toLocation": ["서울허브", "대전물류센터"]}`                                                                                                                                                                 |
| `GET`  | `/api/manager/inventory`| 재고 분포 현황 조회                   | `{"inventoryDistribution": [{"businessStep": "Factory", "value": 1500}, {"businessStep": "WMS", "value": 800}]}`                                                                                                      |
| `GET`  | `/api/manager/byproduct`| 부산물 현황 조회                      | `{"byProductList": [{"productName": "상품A", "fake": 10, "tamper": 5, "clone": 2, "other": 0, "total": 17}, ...]}`                                                                                                |
| `GET`  | `/api/manager/kpi`      | KPI 조회                            | `{"kpiId": 1, "anomalyCount": 125, "anomalyRate": 0.0146, "avgLeadTime": 12.5, "codeCount": 900000, "dispatchRate": 95.1, "inventoryRate": 78.2, "salesRate": 92.5, "totalTripCount": 854320, "uniqueProductCount": 128, "fileId": 101}` |

---

## 4. Admin API (Admin 권한)

### `AdminUserController`

#### **4.1. 전체 사용자 목록 조회**
*   `GET /api/admin/users`
*   **설명:** 시스템에 등록된 모든 사용자 목록을 조회합니다.
*   **Success Response (200 OK):**
    *   **Body (`application/json`):**
        ```json
        {
          "users": [
            {
              "role": "ROLE_MANAGER",
              "locationId": 1001,
              "userId": "manager1",
              "userName": "홍길동",
              "email": "manager1@test.com",
              "status": "active",
              "createdAt": "2024-08-01T10:00:00"
            },
            {
              "role": "ROLE_UNAUTH",
              "locationId": 1002,
              "userId": "unauth_user",
              "userName": "이영희",
              "email": "unauth@test.com",
              "status": "pending",
              "createdAt": "2024-08-21T11:00:00"
            }
          ]
        }
        ```

#### **4.2. 사용자 상태 변경**
*   `PATCH /api/admin/users/status`
*   **설명:** 특정 사용자의 상태를 변경합니다.
*   **Request Body (`application/json`):**
    ```json
    {
      "userId": "unauth_user",
      "status": "active"
    }
    ```
*   **Success Response (200 OK):**
    *   **Body (`text/plain`):** `상태가 변경되었습니다.`

#### **4.3. 사용자 소속 공장 변경**
*   `PATCH /api/admin/users/factory`
*   **설명:** 특정 사용자의 소속 공장을 변경합니다.
*   **Request Body (`application/json`):**
    ```json
    {
      "userId": "manager1",
      "locationId": 1002
    }
    ```
*   **Success Response (200 OK):**
    *   **Body (`text/plain`):** `소속 공장이 변경되었습니다.`
