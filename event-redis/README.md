# Redis for Event API

This directory contains Kubernetes manifests for deploying Redis with persistent storage to support the event-api application.

## Components

- **redis.namespace.yaml**: Creates the `event-redis` namespace
- **redis-pvc.yaml**: PersistentVolumeClaim for Redis data storage (5Gi)
- **redis-configmap.yaml**: Redis configuration with persistence settings
- **redis.deployment.yaml**: Redis deployment with persistent volume
- **redis.service.yaml**: ClusterIP service for internal access

## Features

- **Persistent Storage**: 5Gi persistent volume for data durability
- **AOF Persistence**: Append-Only File for write durability
- **RDB Snapshots**: Periodic snapshots (900s/300s/60s intervals)
- **Resource Limits**: Memory (128Mi-512Mi) and CPU (100m-500m) constraints
- **Health Checks**: Liveness and readiness probes configured
- **Memory Policy**: LRU eviction when max memory reached

## Deployment

Apply all manifests in order:

```bash
kubectl apply -f redis.namespace.yaml
kubectl apply -f redis-pvc.yaml
kubectl apply -f redis-configmap.yaml
kubectl apply -f redis.deployment.yaml
kubectl apply -f redis.service.yaml
```

Or apply all at once:

```bash
kubectl apply -f event-redis/
```

## Accessing Redis

### From within the cluster (event-api namespace):

To allow event-api to access Redis, use the service endpoint:

```
redis.event-redis.svc.cluster.local:6379
```

### Create a cross-namespace service reference (if needed):

You can create an ExternalName service in the event-api namespace:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: event-api
spec:
  type: ExternalName
  externalName: redis.event-redis.svc.cluster.local
```

Then event-api can connect to `redis:6379`

## Verify Deployment

```bash
# Check pods
kubectl get pods -n event-redis

# Check PVC
kubectl get pvc -n event-redis

# Test Redis connection
kubectl exec -it -n event-redis deployment/redis -- redis-cli ping
```

## Configuration

Redis is configured with:
- AOF persistence enabled (appendonly.aof)
- RDB snapshots at 900s (1 change), 300s (10 changes), 60s (10000 changes)
- Max memory: 256MB with LRU eviction policy
- Data directory: /data (mounted from PVC)
