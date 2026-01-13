# ArgoCD Deployment for Weather Dashboard

This directory contains the ArgoCD ApplicationSet for deploying the Red Hat Weather Dashboard application to Kubernetes.

## Prerequisites

- Kubernetes cluster (v1.24+)
- ArgoCD installed in the cluster
- kubectl configured to access your cluster
- Git repository accessible from ArgoCD

## Quick Start

### 1. Update Repository URL

Edit `applicationset.yaml` and replace the repository URL:

```yaml
repoURL: https://github.com/YOUR_ORG/redhat-forecasting.git
```

### 2. Deploy the ApplicationSet

```bash
kubectl apply -f argocd/applicationset.yaml
```

### 3. Verify Deployment

```bash
# Check ApplicationSet
kubectl get applicationset -n argocd

# Check Application
kubectl get application -n argocd

# Check deployed resources
kubectl get all -n weather-dashboard-dev
```

## ApplicationSet Features

### Multi-Environment Support

The ApplicationSet uses a list generator to support multiple environments:

- **Dev**: Deploys to `weather-dashboard-dev` namespace
- Can be extended for staging, production, etc.

### Automatic Sync

- **Auto-sync enabled**: Changes in git automatically deploy
- **Self-heal enabled**: Manual changes are reverted
- **Prune enabled**: Deleted resources are removed

### Components Deployed

1. **PostgreSQL Database**
   - StatefulSet with persistent storage
   - Service for internal access
   - ConfigMap and Secret for configuration

2. **Backend Service (Quarkus)**
   - Deployment with 2 replicas (1 in dev)
   - Service for internal access
   - Health checks and resource limits
   - Automatic database connection

3. **Frontend Service (Vue.js)**
   - Deployment with 2 replicas (1 in dev)
   - Service for internal access
   - Ingress for external access
   - Static assets served via nginx

## Configuration

### Adding More Environments

Edit the `generators.list.elements` in `applicationset.yaml`:

```yaml
generators:
  - list:
      elements:
        - cluster: in-cluster
          url: https://kubernetes.default.svc
          environment: dev
          namespace: weather-dashboard-dev
          repoPath: k8s/overlays/dev
        - cluster: in-cluster
          url: https://kubernetes.default.svc
          environment: prod
          namespace: weather-dashboard-prod
          repoPath: k8s/overlays/prod
```

### Deploying to Different Clusters

For multi-cluster deployments, add cluster URLs:

```yaml
- cluster: prod-cluster
  url: https://prod-cluster.example.com
  environment: prod
  namespace: weather-dashboard
  repoPath: k8s/overlays/prod
```

## Accessing the Application

### Frontend

Once deployed, access the frontend via the Ingress:

```bash
# Get ingress details
kubectl get ingress -n weather-dashboard-dev

# Or use port-forward for testing
kubectl port-forward -n weather-dashboard-dev svc/frontend 8080:80
# Access at http://localhost:8080
```

### Backend API

Access the backend API:

```bash
# Port-forward to backend
kubectl port-forward -n weather-dashboard-dev svc/backend 8090:8090
# API at http://localhost:8090/api/weather
# Swagger UI at http://localhost:8090/swagger-ui
```

### Database

Access PostgreSQL directly (for debugging):

```bash
kubectl port-forward -n weather-dashboard-dev svc/postgres 5432:5432
# Connect with: psql -h localhost -U weather -d weather_db
```

## Customization

### Resource Limits

Edit overlay patches to adjust resources:

```yaml
# k8s/overlays/dev/backend-patch.yaml
spec:
  template:
    spec:
      containers:
        - name: backend
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
```

### Ingress Domain

Edit the Ingress to use your domain:

```yaml
# k8s/base/frontend/ingress.yaml
spec:
  rules:
    - host: weather.your-domain.com
```

### Database Storage

Adjust PVC size:

```yaml
# k8s/base/postgres/pvc.yaml
spec:
  resources:
    requests:
      storage: 20Gi  # Increase as needed
```

## Troubleshooting

### Check Application Status

```bash
# View Application in ArgoCD
kubectl get application weather-dashboard-dev -n argocd -o yaml

# View sync status
kubectl describe application weather-dashboard-dev -n argocd
```

### Check Pod Logs

```bash
# Backend logs
kubectl logs -n weather-dashboard-dev -l app=backend -f

# Frontend logs
kubectl logs -n weather-dashboard-dev -l app=frontend -f

# Database logs
kubectl logs -n weather-dashboard-dev -l app=postgres -f
```

### Manual Sync

If auto-sync is not working:

```bash
# Via kubectl
kubectl patch application weather-dashboard-dev -n argocd \
  --type merge -p '{"operation":{"initiatedBy":{"username":"admin"},"sync":{}}}'

# Or use ArgoCD CLI
argocd app sync weather-dashboard-dev
```

## Uninstall

To remove the application:

```bash
# Delete ApplicationSet (this will delete all Applications)
kubectl delete applicationset weather-dashboard -n argocd

# Manually clean up namespace if needed
kubectl delete namespace weather-dashboard-dev
```

## Notes

- The ApplicationSet creates one Application per environment
- Each Application manages its own namespace
- Secrets use plaintext for demo purposes - use SealedSecrets or External Secrets in production
- Default ingress uses `weather.example.com` - update for your domain
- Airport CSV data must be provided separately (see backend README)
