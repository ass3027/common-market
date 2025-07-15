# 쇼핑몰 프로젝트

본 프로젝트는 MSA 환경에서의 기술적 과제를 해결하고 학습하는 것을 목표로 하는 쇼핑몰 애플리케이션입니다.

---

## 핵심 목표

- **MSA (Microservices Architecture)**: 기능별로 서비스를 분리하여 독립적인 개발, 배포, 확장이 가능한 구조를 설계하고 구축합니다.
- **동시성 제어 (Concurrency Control)**: 여러 사용자가 동시에 특정 상품을 주문하는 상황에서 데이터의 일관성을 보장하는 기술을 적용합니다.
- **Redis Caching**: 자주 조회되지만 변경이 적은 데이터(예: 인기 상품, 카테고리 목록)를 Redis에 캐싱하여 시스템의 응답 속도를 향상시키고 DB 부하를 줄입니다.
- **대규모 트래픽 처리**: 갑작스러운 트래픽 증가에도 안정적으로 서비스를 운영할 수 있는 아키텍처를 설계하고 구현합니다.

---

## MSA 아키텍처 (설계안)

각 서비스는 독립된 Spring Boot 애플리케이션으로 구성되며, API Gateway를 통해 외부 요청을 라우팅합니다. 서비스 간 통신은 Feign Client 또는 Kafka와 같은 메시지 큐를 사용합니다.

- **API Gateway**: 인증/인가, 라우팅, 로드 밸런싱 등 공통 기능을 처리합니다.
- **사용자 서비스 (User Service)**: 회원 가입, 로그인, 프로필 관리 등 사용자 관련 기능을 담당합니다.
- **상품 서비스 (Product Service)**: 상품 등록, 조회, 검색, 재고 관리 등 상품 관련 핵심 기능을 담당합니다.
- **주문 서비스 (Order Service)**: 상품 주문, 결제 처리, 주문 내역 관리 등 주문 관련 기능을 담당합니다.
- **알림 서비스 (Notification Service)**: 주문 상태 변경, 프로모션 등 비동기 메시지 기반의 알림 기능을 담당합니다.

---

## 주요 기술 및 구현 전략

1.  **동시성 제어**
    - **Pessimistic Lock (비관적 락)**: 데이터베이스의 `SELECT ... FOR UPDATE`를 사용하여 트랜잭션 충돌을 방지합니다. (예: 주문 처리 시 재고 차감)
    - **Optimistic Lock (낙관적 락)**: JPA의 `@Version`을 사용하여 데이터 변경 시 충돌을 감지하고 처리합니다.
    - **Distributed Lock (분산 락)**: `Redisson`을 활용하여 여러 서버 인스턴스 환경에서 공유 자원에 대한 동시 접근을 제어합니다. (예: 한정 수량 상품 동시 주문 요청)

2.  **Redis Caching**
    - **Cache-Aside (Lazy Loading)**: 요청 시 캐시에 데이터가 없으면 DB에서 조회 후 캐시에 저장하는 전략을 기본으로 사용합니다.
    - **Write-Through**: 데이터 변경 시 DB와 캐시를 동시에 업데이트하여 데이터 일관성을 유지합니다. (필요시 적용)
    - **주요 캐싱 대상**: 메인 페이지 상품 목록, 사용자 세션 정보, 자주 변경되지 않는 공지사항 등

3.  **대규모 트래픽 처리**
    - **Load Balancing**: API Gateway (Spring Cloud Gateway) 또는 L4/L7 스위치를 사용하여 각 서비스의 부하를 분산합니다.
    - **Message Queue (Kafka/RabbitMQ)**: 주문 처리, 알림 발송 등 시간이 오래 걸리거나 비동기 처리가 필요한 작업을 메시지 큐에 전달하여 시스템의 응답성을 확보하고 서비스 간 결합도를 낮춥니다.
    - **Database Scaling**: Read Replica를 구성하여 조회(Read) 연산의 부하를 분산시킵니다.

---

## 기술 스택

- **Backend**: Kotlin, Spring Boot, Spring Cloud
- **Database**: MySQL (or PostgreSQL), Redis
- **Infra**: Docker, Kubernetes (예정)
- **ORM**: Spring Data JPA (Hibernate)
- **Build Tool**: Gradle

---

## 프로젝트 실행 방법

```bash
# 1. 프로젝트 클론
git clone https://github.com/your-repo/common-market.git
cd common-market

# 2. Gradle 빌드 및 실행
./gradlew bootRun
```
