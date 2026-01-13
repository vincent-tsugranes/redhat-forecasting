# Podman Setup Guide

This project uses **Podman** instead of Docker for container management. Podman is a daemonless, open-source container engine that's fully compatible with Docker containers.

## Why Podman?

- **Daemonless**: No background daemon required
- **Rootless**: Run containers without root privileges
- **Docker-compatible**: Works with Dockerfiles and docker-compose files
- **Open Source**: Fully open-source, no licensing concerns
- **Secure**: Better security isolation

## Installation

### macOS

```bash
# Install Podman
brew install podman

# Install podman-compose (optional but recommended)
pip3 install podman-compose
```

### Linux

#### Fedora/RHEL/CentOS
```bash
sudo dnf install podman podman-compose
```

#### Ubuntu/Debian
```bash
sudo apt-get update
sudo apt-get install podman
pip3 install podman-compose
```

## Initial Setup (macOS Only)

On macOS, Podman runs in a Linux VM. You need to initialize it:

```bash
# Initialize the Podman machine
podman machine init

# Start the Podman machine
podman machine start

# Verify it's running
podman machine list
```

Expected output:
```
NAME                     VM TYPE     CREATED      LAST UP            CPUS        MEMORY      DISK SIZE
podman-machine-default*  qemu        2 hours ago  Currently running  2           2GiB        100GiB
```

### Optional: Configure Machine Resources

If you need more resources for the VM:

```bash
# Stop the machine
podman machine stop

# Remove the existing machine
podman machine rm

# Create a new machine with custom resources
podman machine init --cpus 4 --memory 4096 --disk-size 120

# Start the new machine
podman machine start
```

## Verify Installation

```bash
# Check Podman version
podman --version

# Check podman-compose version
podman-compose --version

# Run a test container
podman run --rm hello-world

# Check running containers
podman ps
```

## Start/Stop Podman Machine (macOS)

```bash
# Start the machine
podman machine start

# Stop the machine (when not needed)
podman machine stop

# Check machine status
podman machine list
```

**Note**: The Podman machine must be running before you can use containers. The `start-all-services.sh` script will check if Podman is available.

## Troubleshooting

### "Cannot connect to Podman" error

On macOS, make sure the Podman machine is running:
```bash
podman machine start
```

### Port conflicts

This project uses **port 5438** for PostgreSQL (instead of the default 5432) to avoid conflicts with existing PostgreSQL installations.

If you still have port conflicts:
```bash
# Check what's using port 5438
lsof -i :5438

# Stop any conflicting services
```

### Permission errors (Linux)

Enable rootless mode:
```bash
# Add your user to the podman group
sudo usermod -aG podman $USER

# Log out and back in for changes to take effect
```

### Volume mount issues (macOS)

Podman on macOS uses a VM, so file paths need to be accessible. By default, only your home directory is mounted. The project scripts handle this automatically.

## Podman vs Docker Commands

Podman commands are compatible with Docker:

| Docker Command | Podman Equivalent |
|---------------|-------------------|
| `docker ps` | `podman ps` |
| `docker build` | `podman build` |
| `docker run` | `podman run` |
| `docker exec` | `podman exec` |
| `docker-compose up` | `podman-compose up` or `podman compose up` |
| `docker logs` | `podman logs` |

## Using Podman with This Project

Once Podman is installed and running:

```bash
# Navigate to the project
cd /Users/vtsugran/Code/redhat-forecasting

# Start all services (handles Podman automatically)
./start-all-services.sh

# Check status
./status.sh

# View logs
./view-logs.sh

# Stop all services
./stop-all-services.sh
```

## Switching from Docker to Podman

If you previously used Docker with this project:

1. **Stop Docker containers**:
   ```bash
   cd redhat-weather-service
   docker-compose down -v
   ```

2. **Install Podman** (see above)

3. **Start with Podman**:
   ```bash
   cd /Users/vtsugran/Code/redhat-forecasting
   ./start-all-services.sh
   ```

The scripts automatically detect and use Podman.

## Additional Resources

- [Podman Official Documentation](https://docs.podman.io/)
- [Podman Desktop](https://podman-desktop.io/) - GUI for Podman (optional)
- [Migration from Docker to Podman](https://docs.podman.io/en/latest/markdown/podman-docker.1.html)

## Support

If you encounter issues:
1. Check `./status.sh` to see which services are running
2. View logs with `./view-logs.sh`
3. Verify Podman is running: `podman machine list` (macOS)
4. Check Podman version: `podman --version` (should be 4.0+)
