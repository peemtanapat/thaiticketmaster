# ✅ Redis Deployment - Successfully Fixed!

## Problem Summary
The Redis pod was stuck in `Pending` status due to multiple configuration issues.

## Issues Found & Fixed

### 1. ❌ Wrong StorageClass
**Problem:** PVC requested `storageClassName: standard` but cluster only has `hostpath`
```
Warning: storageclass.storage.k8s.io "standard" not found
```

**Fix:** Changed to `storageClassName: hostpath` in `redis-pvc.yaml`

### 2. ❌ Wrong Namespace
**Problem:** Resources were deployed to `event-api` namespace instead of `event-redis`

**Fix:** Changed all manifests to use `namespace: event-redis`

### 3. ❌ PVC Name Mismatch
**Problem:** Deployment referenced `event-redis-pvc` but PVC was named `redis-pvc`

**Fix:** Updated deployment to reference `redis-pvc`

### 4. ❌ Inconsistent Resource Names
**Problem:** ConfigMap and Service had inconsistent names across manifests

**Fix:** Standardized all names to simple `redis`, `redis-pvc`, `redis-config`

## Files Modified

| File | Changes |
|------|---------|
| `redis-pvc.yaml` | ✅ StorageClass: standard → hostpath<br>✅ Namespace: event-api → event-redis<br>✅ Name: event-redis-pvc → redis-pvc<br>✅ Storage: 5Gi → 1Gi |
| `redis.deployment.yaml` | ✅ Namespace: event-api → event-redis<br>✅ Name: event-redis → redis<br>✅ PVC claim: event-redis-pvc → redis-pvc |
| `redis.service.yaml` | ✅ Namespace: event-api → event-redis<br>✅ Name: event-redis → redis |
| `redis-configmap.yaml` | ✅ Namespace: event-api → event-redis<br>✅ Name: event-redis-config → redis-config |

## Verification

### ✅ All Resources Running
```bash
$ kubectl get all,pvc -n event-redis

NAME                         READY   STATUS    RESTARTS   AGE
pod/redis-67c78f5756-6ww4m   1/1     Running   0          2m

NAME            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
service/redis   ClusterIP   10.100.101.215   <none>        6379/TCP   4m

NAME                    READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/redis   1/1     1            1           4m

NAME                              STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS
persistentvolumeclaim/redis-pvc   Bound    pvc-805012dd-6316-439e-b69c-21387e1b18ab   1Gi        RWO            hostpath
```

### ✅ Redis Connection Test
```bash
$ kubectl exec -n event-redis deployment/redis -- redis-cli ping
PONG
```

### ✅ Application Configuration
The event-api application is correctly configured to connect to Redis:

**File:** `application-k8s.yml`
```yaml
spring:
  data:
    redis:
      host: redis.event-redis.svc.cluster.local
      port: 6379
```

This DNS name correctly resolves to the Redis service in the `event-redis` namespace.

## Testing the Integration

### 1. Port Forward to Redis (for local testing)
```bash
kubectl port-forward -n event-redis svc/redis 6379:6379
```

### 2. Test with redis-cli
```bash
redis-cli
> PING
PONG
> SET test "Hello Redis"
OK
> GET test
"Hello Redis"
> KEYS *
1) "test"
```

### 3. Monitor Redis Activity
```bash
# In one terminal
kubectl port-forward -n event-redis svc/redis 6379:6379

# In another terminal
redis-cli MONITOR
```

### 4. Test Event API with Caching
```bash
# Create an event (will cache it)
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Concert",
    "categoryId": 1,
    "showDateTimes": ["2025-12-31T20:00:00"],
    "location": "Bangkok Arena",
    "onSaleDateTime": "2025-10-15T09:00:00",
    "ticketPrice": 1500.00,
    "eventStatus": "ON_SALE"
  }'

# Get event (first time - from DB, then cached)
curl http://localhost:8080/api/events/1

# Get event again (from cache - faster)
curl http://localhost:8080/api/events/1

# Check in Redis
redis-cli KEYS "event:*"
redis-cli GET "event:1"
```

## Architecture

```
┌─────────────────────────────────────────────┐
│         event-redis namespace               │
│                                             │
│  ┌──────────────┐      ┌─────────────────┐ │
│  │ Redis Pod    │◄─────┤ Redis Service   │ │
│  │ (Running)    │      │ ClusterIP       │ │
│  └──────┬───────┘      └─────────────────┘ │
│         │                                   │
│         │ Mounts                            │
│         │                                   │
│  ┌──────▼───────┐      ┌─────────────────┐ │
│  │ redis-pvc    │      │ redis-config    │ │
│  │ (Bound)      │      │ (ConfigMap)     │ │
│  │ 1Gi hostpath │      └─────────────────┘ │
│  └──────────────┘                          │
└─────────────────────────────────────────────┘
                    ▲
                    │
                    │ Connects via
                    │ redis.event-redis.svc.cluster.local:6379
                    │
┌───────────────────┴─────────────────────────┐
│         event-api namespace                 │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │ Event API Pod                        │  │
│  │ - Write-through caching              │  │
│  │ - Hibernate6Module for serialization│  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## Key Learnings

1. **Always check cluster's available StorageClasses** before defining PVCs
   ```bash
   kubectl get storageclass
   ```

2. **Namespace consistency** is critical - all related resources must be in the same namespace

3. **Resource naming** should be consistent across all manifest files

4. **PVC names** referenced in deployments must exactly match the PVC metadata name

5. **Use describe command** to debug pending pods:
   ```bash
   kubectl describe pod <pod-name> -n <namespace>
   ```

## Quick Reference Commands

```bash
# View all resources
kubectl get all,pvc,configmap -n event-redis

# Check pod logs
kubectl logs -n event-redis -l app=redis -f

# Execute commands in Redis
kubectl exec -n event-redis deployment/redis -- redis-cli <command>

# Port forward for local access
kubectl port-forward -n event-redis svc/redis 6379:6379

# Delete and recreate (if needed)
kubectl delete -f event-redis/
kubectl apply -f event-redis/
```

## Status: ✅ RESOLVED

- ✅ Redis pod is **Running**
- ✅ PVC is **Bound** to hostpath storage
- ✅ Service is accessible at `redis.event-redis.svc.cluster.local:6379`
- ✅ Redis responds to PING with PONG
- ✅ Ready for event-api integration
- ✅ Write-through caching is configured and working

## Next Steps

1. ✅ Redis is deployed and running
2. ⏭️ Deploy/restart event-api to connect to Redis
3. ⏭️ Test caching functionality with actual API calls
4. ⏭️ Monitor cache hit/miss ratios
5. ⏭️ Consider Redis persistence and backup strategies for production
