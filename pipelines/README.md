# OpenShift Pipelines (Tekton) Configuration

This directory contains the Tekton/OpenShift Pipelines configuration for building and deploying the Weather Dashboard application.

## Overview

The pipeline automates the following steps:
1. Clone the Git repository
2. Build the backend container image using Buildah
3. Build the frontend container image using Buildah
4. Push both images to the container registry (Quay.io)
5. Update Kubernetes manifests with new image tags (optional)

## Prerequisites

### 1. OpenShift Pipelines Operator

Install the OpenShift Pipelines Operator from the OperatorHub:

```bash
# Or install via CLI
oc apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-pipelines-operator
  namespace: openshift-operators
spec:
  channel: latest
  name: openshift-pipelines-operator-rh
  source: redhat-operators
  sourceNamespace: openshift-marketplace
EOF
```

### 2. Create Namespace

```bash
oc create namespace weather-dashboard-dev
```

### 3. Container Registry Credentials

Create a secret with your Quay.io credentials:

```bash
# Generate a random webhook secret
WEBHOOK_SECRET=$(openssl rand -hex 20)

# Create the registry credentials secret
oc create secret docker-registry registry-credentials \
  --docker-server=quay.io \
  --docker-username=YOUR_QUAY_USERNAME \
  --docker-password=YOUR_QUAY_PASSWORD \
  --docker-email=YOUR_EMAIL \
  -n weather-dashboard-dev
```

Or use the template file:

```bash
# Edit registry-secret-template.yaml with your credentials
oc apply -f registry-secret-template.yaml
```

### 4. GitHub Webhook Secret

Create a secret for GitHub webhook authentication:

```bash
# Generate a random webhook secret
WEBHOOK_SECRET=$(openssl rand -hex 20)

# Create the secret
oc create secret generic github-webhook-secret \
  --from-literal=token=$WEBHOOK_SECRET \
  -n weather-dashboard-dev

# Save the token for GitHub webhook configuration
echo "GitHub Webhook Secret: $WEBHOOK_SECRET"
```

## Installation

### 1. Apply Service Account and RBAC

```bash
oc apply -f serviceaccount.yaml
```

### 2. Apply Pipeline Definition

```bash
oc apply -f pipeline.yaml
```

### 3. Apply Triggers (for automated builds)

```bash
oc apply -f triggers/trigger-binding.yaml
oc apply -f triggers/trigger-template.yaml
oc apply -f triggers/event-listener.yaml
```

### 4. Get the Webhook URL

```bash
# Get the EventListener route
oc get route weather-dashboard-webhook -n weather-dashboard-dev

# The URL will be something like:
# https://weather-dashboard-webhook-weather-dashboard-dev.apps.YOUR_CLUSTER_DOMAIN/
```

### 5. Configure GitHub Webhook

1. Go to your GitHub repository settings
2. Navigate to Webhooks > Add webhook
3. Configure:
   - **Payload URL**: Use the route from step 4
   - **Content type**: application/json
   - **Secret**: Use the webhook secret from Prerequisites step 4
   - **Events**: Just the push event
   - **Active**: checked
4. Save the webhook

## Usage

### Manual Pipeline Run

To manually trigger a pipeline build:

```bash
oc create -f pipelinerun.yaml
```

### Automated Builds

Once the GitHub webhook is configured, pushes to the main branch will automatically trigger pipeline runs.

### Monitoring Pipeline Runs

```bash
# List all pipeline runs
oc get pipelineruns -n weather-dashboard-dev

# Watch a specific pipeline run
oc logs -f pipelinerun/PIPELINE_RUN_NAME -n weather-dashboard-dev

# Get detailed status
oc describe pipelinerun/PIPELINE_RUN_NAME -n weather-dashboard-dev
```

### Using Tekton CLI (tkn)

If you have the Tekton CLI installed:

```bash
# List pipelines
tkn pipeline list -n weather-dashboard-dev

# Start a pipeline manually
tkn pipeline start weather-dashboard-pipeline \
  --workspace name=shared-workspace,volumeClaimTemplateFile=- \
  --workspace name=docker-credentials,secret=registry-credentials \
  --serviceaccount=pipeline-sa \
  -n weather-dashboard-dev

# List pipeline runs
tkn pipelinerun list -n weather-dashboard-dev

# View logs
tkn pipelinerun logs -f -n weather-dashboard-dev
```

## Pipeline Parameters

The pipeline accepts the following parameters:

- `git-url`: Git repository URL (default: https://github.com/vincent-tsugranes/redhat-forecasting.git)
- `git-revision`: Git branch/tag/commit (default: main)
- `image-registry`: Container registry URL (default: quay.io)
- `image-namespace`: Registry namespace/username (default: vincent-tsugranes)
- `backend-image-name`: Backend image name (default: weather-backend)
- `frontend-image-name`: Frontend image name (default: weather-frontend)
- `image-tag`: Image tag (default: latest, automatically set to git commit SHA by triggers)

## Workspaces

The pipeline uses two workspaces:

1. **shared-workspace**: Temporary storage for cloned repository and build artifacts (2Gi PVC)
2. **docker-credentials**: Secret containing container registry credentials

## Tasks

The pipeline consists of four tasks:

1. **fetch-repository**: Clones the Git repository using the `git-clone` ClusterTask
2. **build-backend**: Builds and pushes the backend image using the `buildah` ClusterTask
3. **build-frontend**: Builds and pushes the frontend image using the `buildah` ClusterTask
4. **update-manifests**: Updates Kubernetes manifests with new image tags using the `git-cli` ClusterTask (optional)

## Troubleshooting

### Pipeline Run Fails

Check the logs for the failed task:

```bash
oc logs -f pipelinerun/PIPELINE_RUN_NAME -n weather-dashboard-dev
```

### Registry Authentication Issues

Verify the registry credentials secret:

```bash
oc get secret registry-credentials -n weather-dashboard-dev -o yaml
```

### Webhook Not Triggering

Check the EventListener logs:

```bash
oc logs -l eventlistener=weather-dashboard-event-listener -n weather-dashboard-dev
```

Verify the webhook secret matches:

```bash
oc get secret github-webhook-secret -n weather-dashboard-dev -o jsonpath='{.data.token}' | base64 -d
```

### Build Failures

Common issues:
- Insufficient resources: Increase PVC size or pod resources
- Network issues: Check cluster network policies
- Dockerfile errors: Validate Dockerfiles locally

## Files

- `pipeline.yaml`: Main Tekton Pipeline definition
- `pipelinerun.yaml`: Template for manual pipeline execution
- `serviceaccount.yaml`: ServiceAccount and RBAC for pipeline execution
- `registry-secret-template.yaml`: Template for container registry credentials
- `triggers/trigger-binding.yaml`: Extracts data from GitHub webhook payload
- `triggers/trigger-template.yaml`: Template for creating PipelineRuns from triggers
- `triggers/event-listener.yaml`: Receives GitHub webhooks and triggers pipeline runs
- `triggers/github-webhook-secret-template.yaml`: Template for GitHub webhook secret

## Integration with ArgoCD

The pipeline updates the Kustomize manifests in `k8s/overlays/dev/kustomization.yaml` with new image tags. ArgoCD automatically detects these changes and deploys the new images to the cluster.

The integration flow:
1. Push to GitHub main branch
2. GitHub webhook triggers pipeline
3. Pipeline builds images and pushes to Quay.io
4. Pipeline updates image tags in Git repository
5. ArgoCD detects changes and syncs to cluster
6. New pods are deployed with updated images
