# Kubernetes Manifests for Weather Dashboard

This directory contains Kubernetes manifests for deploying the Red Hat Weather Dashboard application using Kustomize.

## Directory Structure

```
k8s/
├── base/                    # Base manifests for all environments
│   ├── postgres/           # PostgreSQL database
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   ├── pvc.yaml
│   │   ├── service.yaml
│   │   ├── statefulset.yaml
│   │   └── kustomization.yaml
│   ├── backend/            # Quarkus backend service
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── kustomization.yaml
│   ├── frontend/           # Vue.js frontend
│   │   ├── configmap.yaml
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── ingress.yaml
│   │   └── kustomization.yaml
│   └── kustomization.yaml  # Base composition
└── overlays/               # Environment-specific customizations
    └── dev/                # Development environment
        ├── backend-patch.yaml
        ├── frontend-patch.yaml
        └── kustomization.yaml
```

## Deployment Options

### Option 1: ArgoCD (Recommended)

Use the ArgoCD ApplicationSet for GitOps deployment:

```bash
kubectl apply -f argocd/applicationset.yaml
```

See [argocd/README.md](../argocd/README.md) for details.

### Option 2: kubectl with Kustomize

Deploy directly using kubectl:

```bash
# Deploy dev environment
kubectl apply -k k8s/overlays/dev

# Deploy base (production-like)
kubectl apply -k k8s/base
```

### Option 3: Kustomize CLI

Build and apply separately:

```bash
# Build manifests
kustomize build k8s/overlays/dev > manifests.yaml

# Review
cat manifests.yaml

# Apply
kubectl apply -f manifests.yaml
```

## Components

### PostgreSQL Database

- **Type**: StatefulSet
- **Storage**: 10Gi PersistentVolumeClaim
- **Image**: postgres:15-alpine
- **Port**: 5432
- **Database**: weather_db
- **User**: weather
- **Password**: weather (change in production!)

### Backend Service

- **Type**: Deployment
- **Replicas**: 2 (1 in dev)
- **Image**: Quarkus native (customize as needed)
- **Port**: 8090
- **Features**:
  - REST API for weather data
  - Automatic database migrations (Flyway)
  - Health checks at `/q/health/*`
  - Swagger UI at `/swagger-ui`

### Frontend Service

- **Type**: Deployment
- **Replicas**: 2 (1 in dev)
- **Image**: nginx:1.25-alpine
- **Port**: 80
- **Features**:
  - Vue.js SPA
  - Nginx static file serving
  - Ingress for external access

## Customization

### Environment Variables

Modify ConfigMaps to change application behavior:

```yaml
# k8s/base/backend/configmap.yaml
data:
  QUARKUS_HTTP_PORT: "8090"
  AIRPORT_CSV_LOAD_ON_STARTUP: "true"
  # Add more as needed
```

### Resource Limits

Adjust in overlays or base:

```yaml
# k8s/overlays/dev/backend-patch.yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### Scaling

Change replicas:

```yaml
# In overlay kustomization.yaml
replicas:
  - name: backend
    count: 3
```

Or via kubectl:

```bash
kubectl scale deployment backend -n weather-dashboard --replicas=3
```

### Image Tags

Update images in overlay:

```yaml
# k8s/overlays/dev/kustomization.yaml
images:
  - name: quay.io/quarkus/ubi-quarkus-native-binary-s2i
    newTag: "2.1"
```

## Creating New Overlays

To create a production overlay:

```bash
mkdir -p k8s/overlays/prod
cd k8s/overlays/prod
```

Create `kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: weather-dashboard-prod

bases:
  - ../../base

namePrefix: prod-

commonLabels:
  environment: prod

replicas:
  - name: backend
    count: 3
  - name: frontend
    count: 3

patchesStrategicMerge:
  - backend-patch.yaml
  - frontend-patch.yaml
```

## Secrets Management

**⚠️ Security Warning**: Current secrets use plaintext for demo purposes.

For production, use one of:

1. **Sealed Secrets**:
   ```bash
   kubeseal --format yaml < secret.yaml > sealed-secret.yaml
   ```

2. **External Secrets Operator**:
   ```yaml
   apiVersion: external-secrets.io/v1beta1
   kind: ExternalSecret
   ```

3. **HashiCorp Vault**

4. **Cloud provider secrets** (AWS Secrets Manager, Azure Key Vault, GCP Secret Manager)

## Namespace Management

The base configuration creates resources in the `weather-dashboard` namespace.

Overlays can override:

```yaml
# In overlay kustomization.yaml
namespace: weather-dashboard-prod
```

## Ingress Configuration

Update the Ingress hostname:

```yaml
# k8s/base/frontend/ingress.yaml
spec:
  rules:
    - host: weather.your-domain.com
```

For TLS:

```yaml
spec:
  tls:
    - hosts:
        - weather.your-domain.com
      secretName: weather-tls-cert
```

## Health Checks

### Backend Health Endpoints

- **Liveness**: `GET /q/health/live`
- **Readiness**: `GET /q/health/ready`
- **Full Health**: `GET /q/health`

### Verify Health

```bash
# Port-forward
kubectl port-forward -n weather-dashboard svc/backend 8090:8090

# Check health
curl http://localhost:8090/q/health
```

## Persistent Data

PostgreSQL uses a PersistentVolumeClaim:

```yaml
# k8s/base/postgres/pvc.yaml
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

**Backup Strategy**: Implement regular backups for production:

```bash
# Example backup
kubectl exec -n weather-dashboard postgres-0 -- \
  pg_dump -U weather weather_db > backup.sql
```

## Monitoring

Add Prometheus annotations:

```yaml
# In deployment
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8090"
    prometheus.io/path: "/q/metrics"
```

## Testing Deployment

```bash
# Apply to test namespace
kubectl apply -k k8s/overlays/dev --dry-run=server

# Actually deploy
kubectl apply -k k8s/overlays/dev

# Watch rollout
kubectl rollout status deployment/backend -n weather-dashboard-dev
kubectl rollout status deployment/frontend -n weather-dashboard-dev

# Check all resources
kubectl get all -n weather-dashboard-dev
```

## Cleanup

```bash
# Delete specific environment
kubectl delete -k k8s/overlays/dev

# Or delete namespace
kubectl delete namespace weather-dashboard-dev
```

## Troubleshooting

### Pods not starting

```bash
# Describe pod
kubectl describe pod <pod-name> -n weather-dashboard-dev

# Check logs
kubectl logs <pod-name> -n weather-dashboard-dev

# Check events
kubectl get events -n weather-dashboard-dev --sort-by='.lastTimestamp'
```

### Database connection issues

```bash
# Test from backend pod
kubectl exec -it <backend-pod> -n weather-dashboard-dev -- \
  nc -zv postgres 5432

# Check service
kubectl get svc postgres -n weather-dashboard-dev
```

### Storage issues

```bash
# Check PVC
kubectl get pvc -n weather-dashboard-dev

# Check PV
kubectl get pv

# Describe for details
kubectl describe pvc postgres-pvc -n weather-dashboard-dev
```

## Next Steps

1. Customize images with your actual container registry
2. Update ingress domain
3. Configure TLS certificates
4. Set up proper secrets management
5. Add monitoring and alerting
6. Configure backup strategy
7. Set up CI/CD pipeline
