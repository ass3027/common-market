# Redis Caching Learning Plan for MSA Project

> A structured learning path for implementing Redis caching in the Common Market application, progressing from basics to production-ready patterns.

---

## Phase 1: Foundation (Week 1-2)

### Learning Objectives
- Understand Spring Cache abstraction
- Set up Redis connection and basic configuration
- Implement simple caching for Product service
- Monitor cache hits/misses

### Practical Implementation Tasks

#### Task 1.1: Redis Setup
- [ ] Install Redis locally (Windows: WSL2 + Redis, or Redis Windows port)
- [ ] Configure `RedisConfig.kt` with basic connection settings
- [ ] Add Redis dependencies to `build.gradle.kts`
- [ ] Verify connection with RedisTemplate operations

#### Task 1.2: Enable Spring Cache
```kotlin
// Config class
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig())
            .build()
    }
}
```

#### Task 1.3: Add @Cacheable to Product Service
- [ ] Cache `getProductById(id)` method
- [ ] Use key: `product::{#id}`
- [ ] Set TTL to 5 minutes for initial testing

```kotlin
@Cacheable(value = ["products"], key = "#productId")
fun getProductById(productId: Long): ProductResponse {
    // existing implementation
}
```

#### Task 1.4: Cache Eviction
- [ ] Add `@CacheEvict` on update/delete operations
- [ ] Test cache invalidation behavior

### Testing Exercises
- [ ] Write test to verify cache hit (should not call repository twice)
- [ ] Write test to verify cache miss on first call
- [ ] Test cache eviction on product update
- [ ] Monitor Redis using `redis-cli MONITOR` command

### Common Issues & Solutions
- **Issue**: Serialization errors with Kotlin data classes
  - **Solution**: Configure Jackson with Kotlin module
- **Issue**: Cache not working in tests
  - **Solution**: Use `@EnableCaching` in test configuration or use embedded Redis

### References
- [Spring Cache Abstraction Docs](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)

---

## Phase 2: Cache Strategies (Week 3-4)

### Learning Objectives
- Understand different caching patterns
- Implement appropriate TTL strategies
- Design effective cache keys
- Handle cache stampede problem

### Practical Implementation Tasks

#### Task 2.1: Cache-Aside Pattern (Current Implementation)
- [ ] Document when Product service reads: check cache → miss → load from DB → store in cache
- [ ] Document when Product service writes: update DB → invalidate cache
- [ ] Understand trade-offs: simple but can have stale data window

#### Task 2.2: Write-Through Pattern (Optional Exploration)
```kotlin
@CachePut(value = ["products"], key = "#result.id")
fun updateProduct(request: ProductUpdateRequest): ProductResponse {
    // Update DB and cache simultaneously
}
```
- [ ] Compare with @CacheEvict approach
- [ ] Understand when to use each

#### Task 2.3: TTL Management Strategy
- [ ] Set different TTLs for different data types:
  - Product details: 10-30 minutes (changes infrequently)
  - Product list: 5 minutes (changes more often)
  - User session: 1 hour
- [ ] Implement custom TTL per cache entry

```kotlin
@Bean
fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
    val cacheConfigurations = mapOf(
        "products" to RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)),
        "productList" to RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
    )

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig())
        .withInitialCacheConfigurations(cacheConfigurations)
        .build()
}
```

#### Task 2.4: Cache Key Design Patterns
- [ ] Use consistent naming convention: `{domain}::{entity}::{id}`
  - Example: `product::detail::123`, `product::list::page:1:size:20`
- [ ] Include relevant parameters in key for filtered lists
- [ ] Avoid overly granular keys (cache explosion)

#### Task 2.5: Cache Stampede Prevention
**Problem**: When cache expires, multiple requests hit DB simultaneously

**Solutions to implement:**
```kotlin
// Option 1: Synchronized caching (Spring's default @Cacheable with sync=true)
@Cacheable(value = ["products"], key = "#id", sync = true)
fun getProductById(id: Long): ProductResponse

// Option 2: Probabilistic early expiration
// Refresh cache before TTL expires for hot keys
```

- [ ] Test stampede scenario with JMeter or similar
- [ ] Implement solution for product listing endpoint
- [ ] Monitor query count during cache expiration

### Testing Exercises
- [ ] Load test with 100 concurrent requests to same product
- [ ] Verify only 1 DB query when using `sync = true`
- [ ] Test cache key generation with different parameters
- [ ] Test TTL expiration timing

### Common Issues & Solutions
- **Issue**: Cache growing unbounded
  - **Solution**: Set maxmemory and eviction policy (LRU)
- **Issue**: Wrong data cached due to key collision
  - **Solution**: Include all relevant params in key
- **Issue**: Cache stampede on popular items
  - **Solution**: Use sync=true or implement cache warming

### References
- [Caching Patterns](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)
- [Cache Stampede Solutions](https://instagram-engineering.com/thundering-herds-promises-82191c8af57d)

---

## Phase 3: Advanced Patterns (Week 5-6)

### Learning Objectives
- Handle distributed caching challenges
- Choose appropriate serialization strategy
- Implement cache consistency across services
- Use distributed locks for critical operations
- Leverage Pub/Sub for cache invalidation

### Practical Implementation Tasks

#### Task 3.1: Serialization Strategy
**Current**: Spring uses Java serialization by default (inefficient)

**Implement JSON serialization:**
```kotlin
@Bean
fun redisCacheConfiguration(): RedisCacheConfiguration {
    val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    return RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                GenericJackson2JsonRedisSerializer(objectMapper)
            )
        )
}
```

**Comparison table to create:**
| Serialization | Pros | Cons | Use Case |
|---------------|------|------|----------|
| Java (default) | Built-in | Large size, Java-only | Never use |
| JSON | Human-readable, language-agnostic | Slower, larger than binary | Development, debugging |
| Protobuf | Fast, compact | Requires schema | High-performance production |
| Kryo | Fastest for Java/Kotlin | JVM-only | Internal services |

- [ ] Implement JSON serialization
- [ ] Compare sizes: Java vs JSON for ProductResponse
- [ ] (Optional) Try Kryo for performance comparison

#### Task 3.2: Cache Consistency Patterns

**Problem**: When Product service updates a product, how do other services know?

**Solutions:**

**Option A: TTL-based eventual consistency (simplest)**
- Accept stale data for TTL duration
- Works when: perfect consistency not required

**Option B: Pub/Sub for cache invalidation**
```kotlin
// In Product Service after update:
@Service
class ProductService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    fun updateProduct(request: ProductUpdateRequest): ProductResponse {
        val product = productRepository.save(...)

        // Publish invalidation event
        redisTemplate.convertAndSend(
            "cache:invalidate:product",
            product.id.toString()
        )

        return product.toResponse()
    }
}

// In any service that caches products:
@Component
class CacheInvalidationListener(
    private val cacheManager: CacheManager
) {
    @RedisMessageListener(topic = "cache:invalidate:product")
    fun onProductInvalidation(productId: String) {
        cacheManager.getCache("products")?.evict(productId)
    }
}
```

- [ ] Implement Pub/Sub for product cache invalidation
- [ ] Test cross-service cache invalidation (simulate with two app instances)

#### Task 3.3: Distributed Locking (Redlock Algorithm)

**Use case**: Prevent duplicate order processing, ensure single execution of scheduled tasks

```kotlin
@Service
class OrderService(
    private val redissonClient: RedissonClient
) {
    fun processOrder(orderId: Long) {
        val lock = redissonClient.getLock("order:lock:$orderId")

        try {
            // Wait up to 10s to acquire, auto-release after 30s
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // Critical section - process order
                // Only one instance can execute this
            } else {
                throw LockAcquisitionException("Could not acquire lock for order $orderId")
            }
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
```

**Tasks:**
- [ ] Add Redisson dependency (easier than manual Redlock)
- [ ] Implement distributed lock for product update operation
- [ ] Test with 2 app instances trying to update same product
- [ ] Understand lock timeout vs lease time

#### Task 3.4: Cache Warming
**Problem**: After deployment, cache is empty → all requests hit DB

**Solutions:**
```kotlin
@Component
class CacheWarmer(
    private val productService: ProductService
) {
    @EventListener(ApplicationReadyEvent::class)
    fun warmCache() {
        // Load top 100 products into cache
        val topProducts = productRepository.findTop100ByOrderByIdDesc()
        topProducts.forEach { product ->
            productService.getProductById(product.id) // Triggers @Cacheable
        }
        log.info("Cache warmed with ${topProducts.size} products")
    }
}
```

- [ ] Implement cache warming for top products
- [ ] Measure startup time impact
- [ ] Consider async warming for large datasets

### Testing Exercises
- [ ] Serialize 1000 ProductResponse objects: compare JSON vs Java size
- [ ] Test Pub/Sub invalidation latency (publish → subscriber receives)
- [ ] Test distributed lock: run 2 instances, verify only 1 processes
- [ ] Test cache warming: verify DB queries only happen once

### Common Issues & Solutions
- **Issue**: Memory usage too high with JSON serialization
  - **Solution**: Use binary format (Kryo/Protobuf) or reduce cached data
- **Issue**: Pub/Sub message lost
  - **Solution**: Use Redis Streams for guaranteed delivery
- **Issue**: Deadlock with distributed locks
  - **Solution**: Always set lease time, use try-finally to unlock

### References
- [Redisson Documentation](https://github.com/redisson/redisson/wiki/Table-of-Content)
- [Redis Pub/Sub](https://redis.io/docs/interact/pubsub/)
- [Redlock Algorithm](https://redis.io/docs/manual/patterns/distributed-locks/)

---

## Phase 4: High Availability (Week 7-8)

### Learning Objectives
- Understand Redis replication architecture
- Set up Redis Sentinel for automatic failover
- Configure Spring Boot for HA Redis
- Test failover scenarios

### Practical Implementation Tasks

#### Task 4.1: Redis Replication Architecture Study

**Concepts to understand:**
- Master-Replica topology (1 master, N replicas)
- Asynchronous replication (eventual consistency)
- Replica serves read-only queries
- All writes go to master

**Replication lag implications:**
- Read from replica might return stale data
- Acceptable for: product listings, non-critical reads
- Not acceptable for: order confirmation, payment status

**Diagram to create:**
```
┌──────────┐  writes   ┌────────────┐
│  Client  │──────────→│   Master   │
└──────────┘           └────────────┘
     │                       │
     │ reads                 │ replication
     ↓                       ↓
┌──────────┐           ┌────────────┐
│ Replica1 │←──────────│  Replica2  │
└──────────┘           └────────────┘
```

- [ ] Document master-replica data flow
- [ ] List scenarios where replication lag matters in e-commerce

#### Task 4.2: Local Redis Sentinel Setup

**Architecture:**
```
Sentinel1   Sentinel2   Sentinel3
    │           │           │
    └───────────┼───────────┘
                │ monitor & decide
                ↓
         ┌────────────┐
         │   Master   │
         └────────────┘
                │
         ┌──────┴──────┐
         ↓             ↓
    Replica1      Replica2
```

**Setup steps:**
```bash
# 1. Create redis.conf for master (port 6379)
port 6379
bind 127.0.0.1

# 2. Create redis-replica1.conf (port 6380)
port 6380
bind 127.0.0.1
replicaof 127.0.0.1 6379

# 3. Create redis-replica2.conf (port 6381)
port 6381
bind 127.0.0.1
replicaof 127.0.0.1 6379

# 4. Create sentinel1.conf (port 26379)
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 10000

# 5. Start all Redis instances and Sentinels
redis-server redis.conf
redis-server redis-replica1.conf
redis-server redis-replica2.conf
redis-sentinel sentinel1.conf
# ... repeat for sentinel2 (26380) and sentinel3 (26381)
```

- [ ] Set up 1 master + 2 replicas locally
- [ ] Set up 3 Sentinel instances (quorum = 2)
- [ ] Verify replication: write to master, read from replica
- [ ] Test manual failover: `SENTINEL FAILOVER mymaster`

#### Task 4.3: Spring Boot Configuration for Sentinel

```yaml
# application.yml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - 127.0.0.1:26379
          - 127.0.0.1:26380
          - 127.0.0.1:26381
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
        cluster:
          refresh:
            adaptive: true  # Auto-discover topology changes
            period: 30s
```

```kotlin
@Configuration
class RedisSentinelConfig {
    @Bean
    fun redisConnectionFactory(
        @Value("\${spring.data.redis.sentinel.master}") masterName: String,
        @Value("\${spring.data.redis.sentinel.nodes}") sentinelNodes: List<String>
    ): RedisConnectionFactory {
        val sentinelConfig = RedisSentinelConfiguration(masterName, HashSet(sentinelNodes))

        val clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .build()

        return LettuceConnectionFactory(sentinelConfig, clientConfig)
    }
}
```

- [ ] Configure application to use Sentinel
- [ ] Implement health check endpoint that verifies Redis connectivity
- [ ] Add retry logic for transient failures

#### Task 4.4: Failover Testing

**Test scenarios:**
1. **Master crashes:**
   - Kill master Redis process
   - Verify Sentinel promotes replica to master (check logs)
   - Verify application still works (may have brief downtime)

2. **Network partition:**
   - Block master with iptables/firewall
   - Verify Sentinel detects and fails over
   - Restore network, observe old master becomes replica

3. **Split-brain prevention:**
   - Partition Sentinels into two groups
   - Verify quorum requirement prevents dual masters

**Test script:**
```kotlin
@SpringBootTest
class RedisSentinelFailoverTest {
    @Autowired
    lateinit var productService: ProductService

    @Test
    fun `should handle master failover gracefully`() {
        // 1. Write to cache
        val product = productService.getProductById(1L)

        // 2. Kill master (manual step or docker stop)
        println("Kill master Redis now! Waiting 10 seconds...")
        Thread.sleep(10000)

        // 3. Verify read still works (from new master)
        val productAfterFailover = productService.getProductById(1L)
        assertEquals(product, productAfterFailover)
    }
}
```

- [ ] Document failover time (down-after-milliseconds + election time)
- [ ] Test application behavior during failover (expect 1-5s errors)
- [ ] Implement circuit breaker pattern for cache operations

#### Task 4.5: Monitoring and Alerting

**Metrics to track:**
- Master/replica status
- Replication lag
- Sentinel quorum status
- Failover events

```kotlin
@Component
class RedisHealthIndicator(
    private val redisTemplate: RedisTemplate<String, String>
) : HealthIndicator {
    override fun health(): Health {
        return try {
            val info = redisTemplate.execute { connection ->
                connection.info("replication")
            }

            Health.up()
                .withDetail("role", extractRole(info))
                .withDetail("connected_slaves", extractSlaveCount(info))
                .build()
        } catch (e: Exception) {
            Health.down(e).build()
        }
    }
}
```

- [ ] Implement Redis health indicator
- [ ] Add metrics for cache hit rate (Micrometer)
- [ ] Set up alerts for: master down, replication lag > 10s

### Testing Exercises
- [ ] Test failover with 1000 req/sec load (JMeter)
- [ ] Measure data loss during failover (writes in-flight)
- [ ] Test network partition scenarios
- [ ] Verify application auto-reconnects to new master

### Common Issues & Solutions
- **Issue**: Quorum not reached, no failover
  - **Solution**: Need at least 3 Sentinels, majority must agree
- **Issue**: Too frequent failovers
  - **Solution**: Increase down-after-milliseconds
- **Issue**: Data loss during failover
  - **Solution**: Use `min-replicas-to-write` on master (blocks writes if replicas disconnected)

### References
- [Redis Sentinel Documentation](https://redis.io/docs/management/sentinel/)
- [Spring Data Redis - Sentinel](https://docs.spring.io/spring-data/redis/reference/redis/sentinel.html)

---

## Phase 5: Production Readiness (Week 9-10)

### Learning Objectives
- Implement comprehensive monitoring
- Tune performance for production load
- Establish testing strategies for cached code
- Handle production incidents (runbooks)
- Understand cost optimization

### Practical Implementation Tasks

#### Task 5.1: Monitoring and Observability

**Metrics to implement (using Micrometer):**

```kotlin
@Configuration
class CacheMetricsConfig(
    private val meterRegistry: MeterRegistry
) {
    @Bean
    fun cacheMetrics(cacheManager: CacheManager): CacheMetrics {
        return CacheMetrics(cacheManager, "product-cache", meterRegistry)
    }
}

@Component
class CacheEventLogger {
    private val hitCounter = meterRegistry.counter("cache.hit", "cache", "product")
    private val missCounter = meterRegistry.counter("cache.miss", "cache", "product")
    private val evictionCounter = meterRegistry.counter("cache.eviction", "cache", "product")

    @EventListener
    fun onCacheHit(event: CacheHitEvent) {
        hitCounter.increment()
    }

    @EventListener
    fun onCacheMiss(event: CacheMissEvent) {
        missCounter.increment()
    }
}
```

**Dashboard metrics:**
- Cache hit ratio: `hit / (hit + miss) * 100%`
- Average cache response time
- Memory usage: `used_memory` / `maxmemory`
- Eviction count (LRU evictions)
- Connection pool stats: active, idle, waiting

**Logging strategy:**
```kotlin
// Aspect for cache operations
@Aspect
@Component
class CacheLoggingAspect {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(cacheable)")
    fun logCacheOperation(joinPoint: ProceedingJoinPoint, cacheable: Cacheable): Any? {
        val start = System.currentTimeMillis()
        val cacheKey = generateKey(joinPoint, cacheable)

        return try {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - start

            log.debug("Cache operation: key={}, duration={}ms", cacheKey, duration)
            result
        } catch (e: Exception) {
            log.error("Cache operation failed: key={}", cacheKey, e)
            throw e
        }
    }
}
```

- [ ] Implement Micrometer metrics for cache hit/miss
- [ ] Set up Grafana dashboard (or Spring Boot Admin)
- [ ] Configure alerts: hit rate < 70%, memory > 80%
- [ ] Add distributed tracing (optional: Zipkin/Jaeger)

#### Task 5.2: Performance Tuning

**Redis configuration tuning:**

```conf
# redis.conf - Production settings

# Memory management
maxmemory 2gb
maxmemory-policy allkeys-lru  # Evict least recently used keys
maxmemory-samples 5           # LRU sample size (higher = more accurate, slower)

# Persistence (choose based on needs)
save ""                       # Disable RDB if using replicas for durability
appendonly yes                # Enable AOF for better durability
appendfsync everysec          # Fsync every second (good balance)

# Performance
tcp-backlog 511
timeout 300                   # Close idle connections after 5min
tcp-keepalive 60

# Replication (if using)
repl-diskless-sync yes        # Faster replication, uses more memory
repl-backlog-size 64mb        # Larger backlog for unstable networks
min-replicas-to-write 1       # Require at least 1 replica for writes
min-replicas-max-lag 10       # Max replication lag in seconds
```

**Spring Boot connection pool tuning:**

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20      # Based on expected concurrent requests
          max-idle: 10        # Keep some connections ready
          min-idle: 5         # Always maintain minimum
          max-wait: 2000ms    # Fail fast if pool exhausted
        shutdown-timeout: 200ms
```

**Performance testing plan:**
- [ ] Benchmark cache operations: GET, SET, DELETE
- [ ] Load test: 1000 req/sec for 10 minutes
- [ ] Identify bottlenecks: CPU, memory, network, connection pool
- [ ] Tune based on results (pool size, timeout, TTL)

#### Task 5.3: Testing Strategies

**Unit tests with embedded Redis:**

```kotlin
// build.gradle.kts
testImplementation("it.ozimov:embedded-redis:0.7.3")

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductServiceCacheTest {
    private lateinit var redisServer: RedisServer

    @BeforeAll
    fun startRedis() {
        redisServer = RedisServer(6370) // Different port
        redisServer.start()
    }

    @AfterAll
    fun stopRedis() {
        redisServer.stop()
    }

    @Test
    fun `should cache product on first call`() {
        // First call - cache miss
        val product1 = productService.getProductById(1L)
        verify(exactly = 1) { productRepository.findById(1L) }

        // Second call - cache hit
        val product2 = productService.getProductById(1L)
        verify(exactly = 1) { productRepository.findById(1L) } // Still 1

        assertEquals(product1, product2)
    }
}
```

**Integration tests with Testcontainers:**

```kotlin
// build.gradle.kts
testImplementation("org.testcontainers:testcontainers:1.19.0")
testImplementation("org.testcontainers:junit-jupiter:1.19.0")

@SpringBootTest
@Testcontainers
class RedisIntegrationTest {
    companion object {
        @Container
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }
    }

    @Test
    fun `should handle Redis connection failure gracefully`() {
        // Test circuit breaker behavior
        redis.stop()

        assertDoesNotThrow {
            productService.getProductById(1L) // Should fallback to DB
        }
    }
}
```

**Testing checklist:**
- [ ] Test cache hit/miss scenarios
- [ ] Test cache eviction on update/delete
- [ ] Test TTL expiration
- [ ] Test serialization/deserialization
- [ ] Test connection failure handling
- [ ] Test concurrent access (race conditions)

#### Task 5.4: Production Runbooks

**Incident: High cache miss rate**

Symptoms:
- Cache hit ratio drops from 80% to 20%
- Database CPU spikes
- Slow response times

Investigation steps:
1. Check Redis memory: `INFO memory`
   - If `used_memory` ≈ `maxmemory` → evictions happening
2. Check eviction count: `INFO stats | grep evicted_keys`
3. Check connection errors in app logs
4. Verify TTL configuration (too short = frequent misses)

Solutions:
- Increase `maxmemory` (scale up Redis)
- Optimize TTL strategy
- Review cached data size (reduce payload)

**Incident: Redis master down**

Symptoms:
- All cache operations failing
- Sentinel logs show failover

Steps:
1. Verify Sentinel promoted new master: `SENTINEL get-master-addr-by-name mymaster`
2. Check application auto-reconnected (should happen automatically)
3. Investigate why master died: check logs, memory, disk
4. Restart old master as replica: `REPLICAOF <new-master-ip> 6379`

**Incident: Replication lag**

Symptoms:
- Replicas are seconds behind master
- Stale data returned to users

Investigation:
1. Check lag: `ROLE` command on replica → `master_repl_offset` diff
2. Check network latency between master and replica
3. Check master write load: `INFO stats | grep instantaneous_ops_per_sec`

Solutions:
- Reduce write load on master (batch operations)
- Increase `repl-backlog-size`
- Use diskless replication for faster sync

- [ ] Create runbook document with common incidents
- [ ] Practice incident response (simulate failures)
- [ ] Document rollback procedures

#### Task 5.5: Cost Optimization

**Strategies to implement:**

1. **Right-size cache:**
   - Monitor actual memory usage over 1 week
   - Set `maxmemory` to 80% of peak usage
   - Estimate: 1GB can cache ~1M ProductResponse objects (JSON)

2. **Optimize serialized data size:**
   ```kotlin
   // Before: 500 bytes per ProductResponse
   data class ProductResponse(
       val id: Long,
       val name: String,
       val price: Long,
       val sellerId: Long,
       val imageUrl: String?,
       val content: String?,  // Large field - 200KB product description
       val createDt: LocalDateTime,
       val updateDt: LocalDateTime
   )

   // After: 200 bytes (cache lightweight version)
   @Cacheable("products")
   fun getProductById(id: Long): ProductCacheDto {
       val product = productRepository.findById(id)
       return ProductCacheDto(
           id = product.id,
           name = product.name,
           price = product.price,
           imageUrl = product.imageUrl
           // Exclude 'content' from cache, load on-demand
       )
   }
   ```

3. **Aggressive TTL for cold data:**
   - Hot products (top 20%): TTL = 1 hour
   - Cold products (bottom 80%): TTL = 5 minutes
   - Implement using `@Caching` with custom TTL logic

4. **Use Redis Cluster only when needed:**
   - Single instance: up to 50k ops/sec
   - Master + replica: sufficient for most use cases
   - Cluster: only if >100k ops/sec or >100GB data

**Cost calculation:**
- Managed Redis (AWS ElastiCache): cache.t3.medium = $0.068/hr ≈ $50/month
- Self-hosted: EC2 t3.medium + Redis = $30/month
- Savings from reduced DB load: priceless 😄

- [ ] Analyze cache memory usage patterns
- [ ] Identify and remove large unused cache keys
- [ ] Implement tiered TTL strategy
- [ ] Document cost-benefit analysis

### Testing Exercises
- [ ] Run 1M requests, measure cost per request with/without cache
- [ ] Simulate production traffic for 24h, analyze metrics
- [ ] Test all runbook procedures
- [ ] Perform chaos engineering: random Redis restarts, network failures

### Common Issues & Solutions
- **Issue**: Cache metrics not showing in Grafana
  - **Solution**: Enable Prometheus endpoint, check Micrometer config
- **Issue**: Memory leak in cache
  - **Solution**: Audit keys without TTL, set default TTL
- **Issue**: High cost
  - **Solution**: Review cache hit rate per key, remove low-value caches

### References
- [Redis Best Practices](https://redis.io/docs/management/optimization/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Testcontainers Documentation](https://www.testcontainers.org/)

---

## Appendix: Quick Reference

### Useful Redis Commands

```bash
# Monitoring
redis-cli INFO                    # All info
redis-cli INFO memory             # Memory usage
redis-cli INFO replication        # Replication status
redis-cli MONITOR                 # Real-time commands (debugging only!)
redis-cli --latency                # Measure latency

# Key operations
redis-cli KEYS "product::*"       # List keys (NEVER in production!)
redis-cli SCAN 0 MATCH "product::*" COUNT 100  # Safe iteration
redis-cli TTL product::123        # Check TTL
redis-cli MEMORY USAGE product::123  # Key memory usage

# Cache operations
redis-cli GET product::123
redis-cli DEL product::123
redis-cli FLUSHDB                 # Delete all keys in current DB (dangerous!)

# Sentinel commands
redis-cli -p 26379 SENTINEL masters
redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
redis-cli -p 26379 SENTINEL failover mymaster
```

### Common @Cacheable Patterns

```kotlin
// Basic caching
@Cacheable(value = ["products"], key = "#id")
fun getProductById(id: Long): ProductResponse

// Conditional caching (only cache if non-null)
@Cacheable(value = ["products"], key = "#id", unless = "#result == null")
fun getProductById(id: Long): ProductResponse?

// Synchronized to prevent cache stampede
@Cacheable(value = ["products"], key = "#id", sync = true)
fun getProductById(id: Long): ProductResponse

// Complex key with multiple params
@Cacheable(value = ["productList"], key = "#sellerId + ':' + #page + ':' + #size")
fun getProductsBySeller(sellerId: Long, page: Int, size: Int): List<ProductResponse>

// Update cache on write
@CachePut(value = ["products"], key = "#result.id")
fun updateProduct(request: ProductUpdateRequest): ProductResponse

// Evict on delete
@CacheEvict(value = ["products"], key = "#id")
fun deleteProduct(id: Long)

// Evict all entries
@CacheEvict(value = ["products"], allEntries = true)
fun deleteAllProducts()

// Multiple cache operations
@Caching(
    evict = [
        CacheEvict(value = ["products"], key = "#id"),
        CacheEvict(value = ["productList"], allEntries = true)
    ]
)
fun updateProduct(id: Long, request: ProductUpdateRequest)
```

### Troubleshooting Checklist

- [ ] Cache not working?
  - Verify `@EnableCaching` is present
  - Check Redis connection: `redis-cli PING`
  - Verify method is called from outside the class (Spring AOP limitation)
  - Check serialization errors in logs

- [ ] Stale data in cache?
  - Verify `@CacheEvict` on update/delete methods
  - Check TTL configuration
  - Consider using Pub/Sub for multi-instance invalidation

- [ ] Performance degradation?
  - Check cache hit rate (should be >70%)
  - Monitor connection pool usage
  - Check for cache stampede (many requests → 1 cache miss)
  - Review serialization overhead (JSON vs binary)

- [ ] Redis out of memory?
  - Check eviction policy: `CONFIG GET maxmemory-policy`
  - Identify large keys: `redis-cli --bigkeys`
  - Reduce TTL or cached data size

---

## Progress Tracking

### Phase 1: Foundation ☐
- [ ] Redis setup complete
- [ ] Basic @Cacheable implemented
- [ ] Cache eviction working
- [ ] Tests passing

### Phase 2: Cache Strategies ☐
- [ ] TTL strategy implemented
- [ ] Cache key design documented
- [ ] Cache stampede prevention tested
- [ ] Write-through pattern explored

### Phase 3: Advanced Patterns ☐
- [ ] JSON serialization configured
- [ ] Pub/Sub invalidation working
- [ ] Distributed locking tested
- [ ] Cache warming implemented

### Phase 4: High Availability ☐
- [ ] Sentinel setup complete
- [ ] Failover tested successfully
- [ ] Monitoring implemented
- [ ] Runbooks created

### Phase 5: Production Readiness ☐
- [ ] Metrics and dashboards live
- [ ] Performance tuned
- [ ] Testing strategy established
- [ ] Cost optimization done

---

## Learning Resources

### Books
- "Redis in Action" by Josiah L. Carlson
- "High Performance Browser Networking" by Ilya Grigorik (caching patterns)

### Online Courses
- Redis University (free courses at university.redis.com)
- Spring Boot Caching (Baeldung tutorials)

### Documentation
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Documentation](https://redis.io/docs/)
- [Lettuce (Redis Client)](https://lettuce.io/)

### Community
- Redis Discord
- r/redis on Reddit
- Stack Overflow [spring-cache] tag

---

**Note**: This plan is designed for self-paced learning. Adjust timelines based on your schedule. Focus on understanding concepts deeply rather than rushing through phases.
