# Redis Pod Pending Issue - Resolution Guide

## Problem
The Redis pod was stuck in `Pending` status with the following issues:

### Issue 1: StorageClass Not Found
```
Warning  ProvisioningFailed  persistentvolume-controller  
storageclass.storage.k8s.io "standard" not found
```

### Issue 2: Wrong Namespace
Redis resources were deployed to `event-api` namespace instead of `event-redis` namespace.

### Issue 3: Inconsistent Resource Names
Resource names didn't match across manifests (e.g., `event-redis-config` vs `redis-config`).

## Root Causes

1. **StorageClass Mismatch**: 
   - PVC requested: `standard`
   - Available in cluster: `hostpath` (default)

2. **Namespace Confusion**:
   - Defined namespace: `event-redis`
   - Actual deployment: `event-api`

3. **Name Inconsistency**:
   - ConfigMap name in deployment: `redis-config`
   - Actual ConfigMap name: `event-redis-config`

## Solutions Applied

### 1. Fixed redis-pvc.yaml
**Changes:**
- ✅ Changed `storageClassName: standard` → `hostpath`
- ✅ Changed `namespace: event-api` → `event-redis`
- ✅ Changed `name: event-redis-pvc` → `redis-pvc`
- ✅ Reduced storage from `5Gi` → `1Gi` (more appropriate for dev/test)

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: event-redis
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
  storageClassName: hostpath
```

### 2. Fixed redis.deployment.yaml
**Changes:**
- ✅ Changed `name: event-redis` → `redis`
- ✅ Changed `namespace: event-api` → `event-redis`

```yaml
metadata:
  name: redis
  namespace: event-redis
```

### 3. Fixed redis.service.yaml
**Changes:**
- ✅ Changed `name: event-redis` → `redis`
- ✅ Changed `namespace: event-api` → `event-redis`

```yaml
metadata:
  name: redis
  namespace: event-redis
```

### 4. Fixed redis-configmap.yaml
**Changes:**
- ✅ Changed `name: event-redis-config` → `redis-config`
- ✅ Changed `namespace: event-api` → `event-redis`

```yaml
metadata:
  name: redis-config
  namespace: event-redis
```

## How to Apply the Fix

### Step 1: Clean up the old resources
```bash
# Delete old resources from event-api namespace
kubectl delete deployment event-redis -n event-api
kubectl delete pvc event-redis-pvc -n event-api
kubectl delete service event-redis -n event-api
kubectl delete configmap event-redis-config -n event-api
```

### Step 2: Apply the corrected manifests
```bash
cd event-redis

# Apply all manifests in correct order
kubectl apply -f redis.namespace.yaml
kubectl apply -f redis-configmap.yaml
kubectl apply -f redis-pvc.yaml
kubectl apply -f redis.deployment.yaml
kubectl apply -f redis.service.yaml
```

### Step 3: Verify deployment
```bash
# Check namespace
kubectl get ns event-redis

# Check all resources
kubectl get all -n event-redis

# Check PVC status (should be Bound)
kubectl get pvc -n event-redis

# Check pod status (should be Running)
kubectl get pods -n event-redis

# View pod details
kubectl describe pod -n event-redis -l app=redis
```

### Step 4: Test Redis connection
```bash
# Port forward to Redis
kubectl port-forward -n event-redis svc/redis 6379:6379

# In another terminal, test connection
redis-cli ping
# Should return: PONG
```

## Expected Results

### Before Fix:
```
NAME                           READY   STATUS    RESTARTS   AGE
event-redis-67c78f5756-vbdm5   0/1     Pending   0          5m

PVC Status: Pending
Events: storageclass.storage.k8s.io "standard" not found
```

### After Fix:
```
NAME                     READY   STATUS    RESTARTS   AGE
redis-6b4bb6b5b5-xxxxx   1/1     Running   0          30s

PVC Status: Bound
Volume: pvc-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

## Verification Checklist

- [ ] Namespace `event-redis` exists
- [ ] PVC `redis-pvc` is in `Bound` state
- [ ] Pod `redis-*` is in `Running` state
- [ ] Service `redis` is accessible
- [ ] ConfigMap `redis-config` exists
- [ ] Redis responds to PING command
- [ ] Application can connect to `redis.event-redis.svc.cluster.local:6379`

## Update Application Configuration

The application is already configured correctly in `application-k8s.yml`:
```yaml
spring:
  data:
    redis:
      host: redis.event-redis.svc.cluster.local
      port: 6379
```

This DNS name resolves to: `redis` service in `event-redis` namespace.

## Common Issues & Solutions

### Issue: PVC still Pending after changing StorageClass
**Solution:** Delete and recreate the PVC
```bash
kubectl delete pvc redis-pvc -n event-redis
kubectl apply -f redis-pvc.yaml
```

### Issue: Pod can't find ConfigMap
**Solution:** Ensure ConfigMap name matches deployment reference
```bash
kubectl get configmap -n event-redis
# Should show: redis-config
```

### Issue: Service DNS not resolving
**Solution:** Check service name and namespace
```bash
kubectl get svc -n event-redis
# Should show: redis
```

## StorageClass Reference

To check available storage classes in your cluster:
```bash
kubectl get storageclass
```

Common storage class names:
- **Docker Desktop / Minikube**: `hostpath`
- **GKE**: `standard`, `standard-rwo`
- **EKS**: `gp2`, `gp3`
- **AKS**: `default`, `managed-premium`

## Files Modified

1. ✅ `redis-pvc.yaml` - Fixed storageClassName and namespace
2. ✅ `redis.deployment.yaml` - Fixed namespace and resource name
3. ✅ `redis.service.yaml` - Fixed namespace and resource name
4. ✅ `redis-configmap.yaml` - Fixed namespace and resource name

## Next Steps

1. Apply the fixes as described above
2. Verify Redis is running
3. Test the event-api application with Redis caching
4. Monitor Redis logs: `kubectl logs -n event-redis -l app=redis -f`
