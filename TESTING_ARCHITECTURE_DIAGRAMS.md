# Testing Architecture & Diagrams

## Test Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   mvn test command                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│  Unit Tests              │  │  Integration Tests       │
│  (Fast, Mocked)          │  │  (Real, Testcontainers) │
├──────────────────────────┤  ├──────────────────────────┤
│ ⚡ < 2 seconds           │  │ 🚀 5-30 seconds          │
│ ❌ No external services  │  │ ✅ Real MongoDB/Kafka    │
│ 🎭 All dependencies      │  │ 📦 Auto container mgmt   │
│    mocked               │  │ 🧹 Auto cleanup         │
│ 🔬 Tests business logic  │  │ 💾 Tests persistence    │
│ 10+ test cases          │  │ 12+ test cases          │
└──────────────────────────┘  └──────────────────────────┘
        ✅ PASSING                 ✅ COMPILING
        (All tests pass)          (Ready to run)
```

---

## Service Architecture with Dependencies

```
┌─────────────────────────────────────────────────────────────────────┐
│                        BACKEND SERVICES                             │
└─────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │  API Gateway    │ (8443)
                    │  (Load Balancer)│
                    └────────┬────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │ User Service │  │Product Service│  │Media Service │
    │   (8081)     │  │   (8082)      │  │  (8083)      │
    └──────┬───────┘  └──────┬────────┘  └──────┬───────┘
           │                 │                 │
           └─────────────────┼─────────────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │  MongoDB     │  │    Kafka     │  │  Discovery   │
    │  (27017)     │  │   (9092)     │  │  Service     │
    └──────────────┘  └──────────────┘  │  (8761)      │
                                         └──────────────┘

All services depend on:
✓ MongoDB (data persistence)
✓ Kafka (event messaging)
✓ Discovery Service (service registry)
```

---

## Docker Compose Startup Sequence

```
┌─────────────────────────────────────────────────────────────┐
│                  docker-compose up                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                    START PHASE 1
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼ WAIT FOR HEALTHY              ▼ WAIT FOR HEALTHY
    ┌─────────────┐              ┌─────────────────────┐
    │  ZOOKEEPER  │              │ MONGODB             │
    │ (port 2181) │              │ (port 27017)        │
    │ healthcheck │◄─────────────│ healthcheck         │
    │    ✓ PASS   │              │    ✓ PASS           │
    └────┬────────┘              └──────┬──────────────┘
         │                               │
    START PHASE 2                   PHASE COMPLETE
         │
         ▼ WAIT FOR HEALTHY
    ┌─────────────┐
    │   KAFKA     │
    │ (port 9092) │
    │ healthcheck │
    │    ✓ PASS   │
    └────┬────────┘
         │
    START PHASE 3
         │
         ▼ WAIT FOR HEALTHY
    ┌─────────────────────┐
    │ DISCOVERY-SERVICE   │
    │    (port 8761)      │
    │ healthcheck         │
    │    ✓ PASS           │
    └────┬────────────────┘
         │
    START PHASE 4
         │
    ┌────┴────┬────────────┬─────────────┐
    │          │            │             │
    ▼          ▼            ▼             ▼
 USER-SRV  PRODUCT-SRV  MEDIA-SRV   DUMMY-DATA
 (8081)     (8082)       (8083)      (init data)
 healthck   healthck     healthck
   ✓          ✓            ✓


ALL SERVICES UP AND HEALTHY ✓
```

---

## Test Execution Phases

```
┌──────────────────────────────────────────────────────────┐
│                  UNIT TEST PHASE                         │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Mockito Setup ────► Mock Creation ────► Test Setup    │
│      │                   │                    │          │
│      ▼                   ▼                    ▼          │
│  @Mock              @InjectMocks          @BeforeEach    │
│  Repository         Service               Create test   │
│  PasswordEncoder    Initialized           data          │
│  KafkaTemplate      with mocks                          │
│                                                          │
│  Then for each test:                                     │
│  ┌─────────────────────────────────────────────────┐    │
│  │  1. When(...).thenReturn(...)  [Arrange]       │    │
│  │  2. Call service method        [Act]            │    │
│  │  3. AssertThat result matches  [Assert]         │    │
│  │  4. Verify mocks were called   [Verify]         │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  ⏱️ Total execution: < 2 seconds for 11 tests           │
│  ✅ Result: ALL PASSING                                 │
└──────────────────────────────────────────────────────────┘


┌──────────────────────────────────────────────────────────┐
│             INTEGRATION TEST PHASE                       │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  @Testcontainers ────► Container Startup               │
│      │                       │                          │
│      ├─────────────────────────────────┐                │
│      │                                  │                │
│      ▼                                  ▼                │
│  MongoDB Container                 Kafka Container      │
│  Docker Image pulled               Docker Image pulled  │
│  Container started                 Container started    │
│  Random port assigned              Random port assigned │
│  (e.g., 32789)                     (e.g., 32790)        │
│                                                          │
│  @DynamicPropertySource injects URIs into Spring:       │
│  ├─ spring.data.mongodb.uri =                           │
│  │  mongodb://localhost:32789/testdb                   │
│  └─ spring.kafka.bootstrap-servers =                    │
│     localhost:32790                                     │
│                                                          │
│  Spring Test Context Initialized with real beans       │
│      │                                                   │
│      ▼                                                   │
│  Tests run with REAL connections:                       │
│  ├─ Save to MongoDB ✓                                   │
│  ├─ Query MongoDB ✓                                     │
│  ├─ Publish to Kafka ✓                                  │
│  └─ Consume from Kafka ✓                                │
│                                                          │
│  @AfterEach Cleanup                                     │
│  ├─ Clear test data from MongoDB                        │
│  └─ Prepare for next test                               │
│                                                          │
│  After All Tests:                                       │
│  ├─ MongoDB container stopped                           │
│  ├─ Kafka container stopped                             │
│  └─ Containers removed                                  │
│                                                          │
│  ⏱️ Total execution: 5-30 seconds for 12 tests          │
│  ✅ Result: READY TO RUN (compiling)                    │
└──────────────────────────────────────────────────────────┘
```

---

## Test vs No-Test Scenarios

```
SCENARIO 1: LOCAL DEVELOPMENT
┌─────────────────────────────────────────────────────────┐
│ No existing services running                            │
├─────────────────────────────────────────────────────────┤
│ mvn test (unit tests only)                              │
│ ✅ Works fine - no external services needed            │
│ ⚡ 2 seconds                                            │
│                                                         │
│ mvn test (all tests including integration)              │
│ ✅ Works fine - Testcontainers starts MongoDB/Kafka    │
│ 🚀 5-30 seconds (depends on image caching)             │
│                                                         │
│ docker-compose up                                       │
│ ✅ Full system running                                  │
│ Manual testing with HTTP requests possible              │
└─────────────────────────────────────────────────────────┘

SCENARIO 2: JENKINS PIPELINE
┌─────────────────────────────────────────────────────────┐
│ Stage 1: Build                                          │
│ mvn clean package -DskipTests                           │
│ ✅ Builds without tests                                 │
│                                                         │
│ Stage 2: Unit Tests                                     │
│ mvn test -Dtest=UserServiceUnitTest                     │
│ ✅ Works - no services needed                           │
│ ⚡ Quick feedback                                       │
│                                                         │
│ Stage 3: Integration Tests                              │
│ mvn test -Dtest=UserServiceIntegrationTest              │
│ ✅ Works - Testcontainers manages containers            │
│ 🚀 Real database/Kafka testing                          │
│                                                         │
│ Stage 4: Docker Build & Deploy                          │
│ docker build & docker-compose up                        │
│ ✅ Full system deployment                               │
│ docker-compose down (cleanup)                           │
└─────────────────────────────────────────────────────────┘

SCENARIO 3: DOCKER COMPOSE FULL STACK
┌─────────────────────────────────────────────────────────┐
│ Terminal 1: docker-compose up                           │
│ ✅ All services running (zookeeper, kafka, mongodb...) │
│                                                         │
│ Terminal 2: mvn test                                    │
│ ✅ Tests can use existing services OR                   │
│ ✅ Can start additional Testcontainers if needed        │
│ 🚀 Full end-to-end testing                              │
│                                                         │
│ Terminal 3: curl http://api                             │
│ ✅ Manual HTTP testing against real system              │
└─────────────────────────────────────────────────────────┘
```

---

## Dependency Resolution During Tests

```
UNIT TEST:
┌────────────────────────────────────┐
│ @Mock UserRepository               │
│ ├─ findById() → returns mock data  │
│ ├─ save() → returns mock object    │
│ └─ findByEmail() → Optional.of()   │
│                                    │
│ @Mock PasswordEncoder              │
│ ├─ encode() → returns "encoded"    │
│ └─ matches() → returns true/false  │
│                                    │
│ All external calls intercepted!    │
│ No actual database access!         │
│ No actual Kafka calls!             │
└────────────────────────────────────┘

INTEGRATION TEST:
┌────────────────────────────────────────┐
│ Testcontainers MongoDB Container       │
│ ├─ Real MongoDB instance               │
│ ├─ Ready for real queries              │
│ └─ Data persisted during test          │
│                                        │
│ Testcontainers Kafka Container         │
│ ├─ Real Kafka instance                 │
│ ├─ Ready for publishing/consuming      │
│ └─ Messages actually sent/received     │
│                                        │
│ Spring beans use real repositories!    │
│ Real database operations!              │
│ Real message publishing!               │
└────────────────────────────────────────┘
```

---

## Test Coverage Matrix

```
              UserService  ProductService  MediaService
              ───────────  ──────────────  ─────────────
Unit Tests         ✅          ( )             ( )
Integration        ✅          ( )             ( )
Controller         ( )          ( )             ( )

Legend:
✅ = Completed
( ) = To be completed (use template)

Strategy:
1. UserService DONE (reference implementation)
2. Copy pattern to ProductService
3. Copy pattern to MediaService
4. Adapt templates for ControllerTests
```

---

## Testcontainers Container Lifecycle

```
TEST CLASS LOAD
       │
       ▼
   @Testcontainers
   @SpringBootTest
       │
       ▼
   INITIALIZATION PHASE
   ┌─────────────────────────────────────────┐
   │ @Container static MongoDBContainer      │
   │ ├─ Check if image exists locally        │
   │ ├─ If not, pull from registry           │
   │ ├─ Create container                     │
   │ └─ Start container on random port       │
   │                                         │
   │ @Container static KafkaContainer        │
   │ ├─ Check if image exists locally        │
   │ ├─ If not, pull from registry           │
   │ ├─ Create container                     │
   │ └─ Start container on random port       │
   └─────────────────────────────────────────┘
       │
       ▼
   CONFIGURATION PHASE
   ┌─────────────────────────────────────────┐
   │ @DynamicPropertySource                  │
   │ Gets container endpoints:               │
   │ ├─ MongoDB: localhost:random_port_1     │
   │ └─ Kafka: localhost:random_port_2       │
   │                                         │
   │ Injects into Spring properties:         │
   │ ├─ spring.data.mongodb.uri              │
   │ └─ spring.kafka.bootstrap-servers       │
   └─────────────────────────────────────────┘
       │
       ▼
   SPRING CONTEXT INITIALIZATION
   ├─ Spring creates beans
   ├─ Beans autowired with real connections
   └─ Ready to run tests!
       │
       ▼
   FIRST @BeforeEach
   ├─ Clear test data
   └─ Initialize test fixtures
       │
       ▼
   TEST EXECUTION 1
   ├─ Use real connections
   ├─ Save to MongoDB
   └─ Query from Kafka
       │
       ▼
   @AfterEach CLEANUP
   ├─ Clear test data
   └─ Close connections
       │
       ▼
   REPEAT FOR EACH TEST
       │
       ▼
   AFTER ALL TESTS
   ┌─────────────────────────────────────────┐
   │ Container Cleanup:                      │
   │ ├─ MongoDB container stopped            │
   │ ├─ Kafka container stopped              │
   │ ├─ Remove containers                    │
   │ └─ Remove temporary volumes (optional)  │
   └─────────────────────────────────────────┘
       │
       ▼
   TEST CLASS UNLOAD
```

---

## Mock vs Real Comparison

```
┌──────────────────────────────────────────────────────────┐
│              UNIT TEST (MOCKED)                          │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  UserService.registerUser()                              │
│          │                                               │
│          ├─ Mock receives: new User(...)                │
│          │                                               │
│          ├─ When userRepository.save() called:           │
│          │   Mock returns: User with id="123"            │
│          │                                               │
│          ├─ When passwordEncoder.encode() called:        │
│          │   Mock returns: "encoded_password"            │
│          │                                               │
│          └─ Returns: User("123", "...encoded...")        │
│                                                          │
│  Assert: registeredUser.getId() == "123" ✅             │
│                                                          │
│  Database: Never touched ✓                              │
│  Kafka: Never called ✓                                  │
│  Speed: < 1ms per call ✓                                │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│          INTEGRATION TEST (TESTCONTAINERS)               │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  UserService.registerUser()                              │
│          │                                               │
│          ├─ Real UserRepository instance                 │
│          │   └─ Connects to MongoDB container            │
│          │                                               │
│          ├─ When save() called:                          │
│          │   └─ Actually inserts into MongoDB            │
│          │      MongoDB generates ObjectID               │
│          │                                               │
│          ├─ When PasswordEncoder.encode() called:        │
│          │   └─ Actually encodes password                │
│          │                                               │
│          └─ Returns: User with real MongoDB ID           │
│                                                          │
│  Assert: registeredUser.getId() != null ✅              │
│  Verify: Data in MongoDB ✅                             │
│                                                          │
│  Database: MongoDB container touched ✓                  │
│  Kafka: Real Kafka container (if used) ✓                │
│  Speed: 10-100ms per call (realistic) ✓                 │
└──────────────────────────────────────────────────────────┘
```

---

## Summary

**Unit Tests** = Fast, Mocked, Local, Always ⚡
**Integration Tests** = Real, Testcontainers, CI/CD 🔧
**Full System** = Docker Compose, Complete, Staging 🐌

Choose based on your needs:
- **Development cycle?** → Unit tests
- **Before merging?** → Unit + Integration tests
- **Full system validation?** → Docker Compose + E2E tests

