# Veccy Kubernetes Deployment

This directory contains Kubernetes manifests for deploying Veccy vector database on Kubernetes.

## Prerequisites

- Kubernetes cluster (v1.24+)
- `kubectl` configured to access your cluster
- Persistent storage provisioner
- (Optional) Ingress controller (nginx recommended)
- (Optional) Prometheus Operator for monitoring
- (Optional) cert-manager for TLS certificates

## Quick Start

### Basic Deployment

```bash
# Deploy all resources
kubectl apply -f k8s/

# Or using kustomize
kubectl apply -k k8s/

# Check deployment status
kubectl get pods -l app=veccy
kubectl get svc -l app=veccy
```

### Verify Deployment

```bash
# Check pod health
kubectl get pods -l app=veccy

# View logs
kubectl logs -l app=veccy --tail=100 -f

# Check health endpoint
kubectl port-forward svc/veccy 8080:8080
curl http://localhost:8080/health
```

## Resource Files

### Core Resources

- **deployment.yaml** - Main Veccy deployment with 3 replicas
- **service.yaml** - ClusterIP service exposing health and metrics ports
- **persistentvolumeclaim.yaml** - 10Gi PVC for data storage

### Configuration

- **configmap.yaml** - Application configuration (Java opts, paths, settings)
- **kustomization.yaml** - Kustomize configuration for managing resources

### Scaling

- **hpa.yaml** - Horizontal Pod Autoscaler (3-10 replicas, 70% CPU target)
- **poddisruptionbudget.yaml** - Ensures minimum 2 pods during disruptions

### Networking

- **ingress.yaml** - External access configuration
- **networkpolicy.yaml** - Network security policies

### Monitoring

- **servicemonitor.yaml** - Prometheus ServiceMonitor for metrics collection

## Configuration Options

### Environment Variables

Set via ConfigMap (`configmap.yaml`):

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | `-Xmx4g -Xms1g -XX:+UseG1GC` | JVM options |
| `VECCY_DATA_DIR` | `/data` | Data storage directory |
| `VECCY_LOG_DIR` | `/logs` | Log output directory |
| `VECCY_HEALTH_PORT` | `8080` | Health check port |
| `VECCY_METRICS_PORT` | `8081` | Metrics endpoint port |
| `VECCY_INDEX_TYPE` | `hnsw` | Default index type |
| `VECCY_METRIC` | `cosine` | Default similarity metric |

### Resource Limits

Adjust in `deployment.yaml`:

```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "6Gi"
    cpu: "2000m"
```

### Storage

Modify in `persistentvolumeclaim.yaml`:

```yaml
spec:
  resources:
    requests:
      storage: 10Gi  # Change as needed
  storageClassName: fast-ssd  # Use your storage class
```

## Deployment Scenarios

### Development/Testing

```bash
# Reduce replicas to 1
kubectl scale deployment veccy --replicas=1

# Use smaller resources
kubectl set resources deployment veccy \
  --requests=cpu=500m,memory=1Gi \
  --limits=cpu=1000m,memory=2Gi
```

### Production

```bash
# Apply all resources including monitoring
kubectl apply -f k8s/

# Enable monitoring profile (if using profiles)
kubectl apply -k k8s/ --profile=monitoring

# Verify HPA is working
kubectl get hpa veccy-hpa
```

### High Availability

```yaml
# Update deployment.yaml
spec:
  replicas: 5  # More replicas

  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:  # Stronger anti-affinity
        - labelSelector:
            matchExpressions:
              - key: app
                operator: In
                values:
                  - veccy
          topologyKey: kubernetes.io/hostname
```

## Monitoring

### Prometheus Metrics

Metrics are exposed on port 8081 at `/metrics`:

```bash
# Port forward to view metrics locally
kubectl port-forward svc/veccy 8081:8081
curl http://localhost:8081/metrics
```

### Health Checks

Veccy provides three health endpoints:

- `/health/live` - Liveness probe (pod should be restarted if fails)
- `/health/ready` - Readiness probe (pod receives traffic when ready)
- `/health` - Full health check with detailed status

### ServiceMonitor

If using Prometheus Operator:

```bash
# Apply ServiceMonitor
kubectl apply -f k8s/servicemonitor.yaml

# Verify Prometheus is scraping
kubectl logs -n monitoring prometheus-xxx -c prometheus | grep veccy
```

## Ingress Configuration

### Update Ingress Host

Edit `ingress.yaml`:

```yaml
spec:
  tls:
    - hosts:
        - veccy.yourdomain.com  # Your domain
      secretName: veccy-tls-secret
  rules:
    - host: veccy.yourdomain.com  # Your domain
```

### Generate TLS Certificate

With cert-manager:

```bash
# Cert-manager will automatically generate certificate
# based on the annotation in ingress.yaml
kubectl get certificate veccy-tls-secret
kubectl describe certificate veccy-tls-secret
```

### Access via Ingress

```bash
# Health check
curl https://veccy.yourdomain.com/health

# Metrics (if exposed)
curl https://veccy.yourdomain.com/metrics
```

## Scaling

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment veccy --replicas=5

# View current replicas
kubectl get deployment veccy
```

### Auto-scaling

HPA is configured to scale based on CPU and memory:

```bash
# View HPA status
kubectl get hpa veccy-hpa

# Describe HPA for details
kubectl describe hpa veccy-hpa

# Adjust HPA
kubectl edit hpa veccy-hpa
```

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -l app=veccy

# View pod events
kubectl describe pod <pod-name>

# Check logs
kubectl logs <pod-name>

# Check previous container logs (if restarted)
kubectl logs <pod-name> --previous
```

### Storage Issues

```bash
# Check PVC status
kubectl get pvc veccy-data-pvc

# Check PV
kubectl get pv

# Describe PVC for events
kubectl describe pvc veccy-data-pvc
```

### Health Check Failures

```bash
# Test health endpoint directly
kubectl exec -it <pod-name> -- curl http://localhost:8080/health/live

# Check liveness probe configuration
kubectl get pod <pod-name> -o yaml | grep -A 10 livenessProbe
```

### Network Issues

```bash
# Test service connectivity
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://veccy:8080/health

# Check network policy
kubectl describe networkpolicy veccy-network-policy

# View service endpoints
kubectl get endpoints veccy
```

## Maintenance

### Rolling Update

```bash
# Update image
kubectl set image deployment/veccy veccy=veccy:v1.1.0

# Watch rollout status
kubectl rollout status deployment/veccy

# View rollout history
kubectl rollout history deployment/veccy
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/veccy

# Rollback to specific revision
kubectl rollout undo deployment/veccy --to-revision=2
```

### Backup Data

```bash
# Create a backup job
kubectl create job veccy-backup-$(date +%Y%m%d) \
  --from=cronjob/veccy-backup  # If you have a backup CronJob

# Or manually backup PVC
kubectl exec <pod-name> -- tar czf - /data > backup.tar.gz
```

## Clean Up

```bash
# Delete all resources
kubectl delete -f k8s/

# Or using kustomize
kubectl delete -k k8s/

# Verify deletion
kubectl get all -l app=veccy

# Delete PVC (data will be lost!)
kubectl delete pvc veccy-data-pvc
```

## Advanced Configuration

### Using Kustomize Overlays

Create environment-specific overlays:

```bash
k8s/
├── base/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── kustomization.yaml
└── overlays/
    ├── development/
    │   ├── kustomization.yaml
    │   └── deployment-patch.yaml
    └── production/
        ├── kustomization.yaml
        └── deployment-patch.yaml

# Deploy development
kubectl apply -k k8s/overlays/development

# Deploy production
kubectl apply -k k8s/overlays/production
```

### StatefulSet Deployment

For stateful deployments with stable network identities:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: veccy
spec:
  serviceName: veccy-headless
  replicas: 3
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
```

## Security Considerations

1. **Run as non-root** - Configured in deployment.yaml
2. **Network policies** - Restrict pod-to-pod communication
3. **RBAC** - Create service account with minimal permissions
4. **Secrets** - Use Kubernetes secrets for sensitive data
5. **Pod Security Standards** - Apply restricted pod security standards

## Performance Tuning

1. **Resource requests/limits** - Set based on workload
2. **Persistent volume** - Use high-performance storage class
3. **Anti-affinity** - Distribute pods across nodes
4. **HPA configuration** - Tune scaling thresholds
5. **Java heap size** - Adjust `JAVA_OPTS` based on pod memory

## Support

For issues and questions:
- GitHub Issues: https://github.com/your-org/veccy/issues
- Documentation: https://github.com/your-org/veccy/docs
