# PostgreSQL for Event API

This directory contains Kubernetes manifests for deploying PostgreSQL with persistent storage to support the event-api application.

## Components

- **postgresql.namespace.yaml**: Creates the `event-db` namespace
- **postgresql-pvc.yaml**: PersistentVolumeClaim for PostgreSQL data storage (10Gi)
- **postgresql-secret.yaml**: Credentials and database configuration
- **postgresql-configmap.yaml**: PostgreSQL performance configuration
- **postgresql.deployment.yaml**: PostgreSQL deployment with persistent volume
- **postgresql.service.yaml**: ClusterIP service for internal access

## Database Configuration

- **Database Name**: `event-db`
- **Username**: `eventuser`
- **Password**: `eventpass123` (⚠️ Change in production!)
- **PostgreSQL Version**: 16 (Alpine)

## Features

- **Persistent Storage**: 10Gi persistent volume for data durability
- **Resource Limits**: Memory (256Mi-1Gi) and CPU (250m-1000m) constraints
- **Health Checks**: Liveness and readiness probes using `pg_isready`
- **Optimized Configuration**: Tuned for moderate workloads
- **Secure Credentials**: Stored in Kubernetes secrets

## Deployment

Apply all manifests in order:

```bash
kubectl apply -f postgresql.namespace.yaml
kubectl apply -f postgresql-secret.yaml
kubectl apply -f postgresql-pvc.yaml
kubectl apply -f postgresql-configmap.yaml
kubectl apply -f postgresql.deployment.yaml
kubectl apply -f postgresql.service.yaml
```

Or apply all at once:

```bash
kubectl apply -f event-db/
```

## Accessing PostgreSQL

### From within the cluster (event-api namespace):

To allow event-api to access PostgreSQL, use the service endpoint:

```
postgresql.event-db.svc.cluster.local:5432
```

### Connection String Format:

```
postgresql://eventuser:eventpass123@postgresql.event-db.svc.cluster.local:5432/event-db
```

Or for JDBC:

```
jdbc:postgresql://postgresql.event-db.svc.cluster.local:5432/event-db
```

### Create a cross-namespace service reference (if needed):

You can create an ExternalName service in the event-api namespace:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: event-api
spec:
  type: ExternalName
  externalName: postgresql.event-db.svc.cluster.local
```

Then event-api can connect to `postgresql:5432`

## Verify Deployment

```bash
# Check pods
kubectl get pods -n event-db

# Check PVC
kubectl get pvc -n event-db

# Test PostgreSQL connection
kubectl exec -it -n event-db deployment/postgresql -- psql -U eventuser -d event-db -c "SELECT version();"

# Check database
kubectl exec -it -n event-db deployment/postgresql -- psql -U eventuser -d event-db -c "\l"
```

## Configuration

PostgreSQL is configured with:
- Max connections: 100
- Shared buffers: 256MB
- Effective cache size: 1GB
- Data directory: /var/lib/postgresql/data/pgdata (mounted from PVC)

## Security Notes

⚠️ **Important**: The default password in `postgresql-secret.yaml` is for development only. 

For production:

1. Generate a strong password:
```bash
openssl rand -base64 32
```

2. Update the secret:
```bash
kubectl create secret generic postgresql-secret \
  --from-literal=POSTGRES_DB=event-db \
  --from-literal=POSTGRES_USER=eventuser \
  --from-literal=POSTGRES_PASSWORD=<your-strong-password> \
  --namespace=event-db \
  --dry-run=client -o yaml | kubectl apply -f -
```

3. Or use a password from file:
```bash
echo -n "your-strong-password" > postgres-password.txt
kubectl create secret generic postgresql-secret \
  --from-literal=POSTGRES_DB=event-db \
  --from-literal=POSTGRES_USER=eventuser \
  --from-file=POSTGRES_PASSWORD=postgres-password.txt \
  --namespace=event-db
```

## Backup Recommendations

```bash
# Create a backup
kubectl exec -n event-db deployment/postgresql -- \
  pg_dump -U eventuser event-db > backup-$(date +%Y%m%d-%H%M%S).sql

# Restore from backup
kubectl exec -i -n event-db deployment/postgresql -- \
  psql -U eventuser -d event-db < backup-20250102-120000.sql
```

## Spring Boot Configuration

Add to your `application.yaml` or `application.properties`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgresql.event-db.svc.cluster.local:5432/event-db
    username: eventuser
    password: eventpass123
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
```

Or use environment variables in your deployment:

```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: jdbc:postgresql://postgresql.event-db.svc.cluster.local:5432/event-db
  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: postgresql-secret
        key: POSTGRES_USER
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: postgresql-secret
        key: POSTGRES_PASSWORD
```
