# PodPirate

## Architecture

The application is split across two machines:

- **Server** — web frontend, backend API, PostgreSQL (`docker-compose.yml`)
- **AI Worker** — Whisper transcription + Ollama ad detection on a GPU machine (`docker-compose.worker.yml`)

The backend communicates with the AI services over HTTP. No shared volumes are needed between the two machines.

## Setup

### AI Worker Machine

**Prerequisites:** NVIDIA GPU, Docker with [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html) installed.

1. Clone the repo (only `whisper-service/` and `docker-compose.worker.yml` are needed).

2. Start the AI services:
   ```sh
   docker compose -f docker-compose.worker.yml up -d
   ```

3. Verify services are running:
   ```sh
   curl http://localhost:8000/health
   curl http://localhost:11435/api/tags
   ```

4. Pull the Ollama model:
   ```sh
   docker compose -f docker-compose.worker.yml exec ollama ollama pull llama3.2
   ```

### Server Machine

1. Copy `.env.example` to `.env` and fill in the values:
   ```sh
   cp .env.example .env
   ```

2. Set `AI_WORKER_HOST` to the worker machine's LAN IP and choose a secure `DB_PASSWORD`.

3. Start the server stack:
   ```sh
   docker compose up -d
   ```

4. The web UI is available at `http://localhost:3000`.

## Troubleshooting

- **Whisper won't start on worker** — Verify GPU access: `docker run --rm --gpus all nvidia/cuda:12.4.1-cudnn-runtime-ubuntu22.04 nvidia-smi`
- **Backend can't reach worker** — Check that the firewall allows ports `8000` and `11435` from the server machine.
- **No Ollama model** — The model must be pulled manually after first deploy (see step 4 above).
