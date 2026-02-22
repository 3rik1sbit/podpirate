# Deploy PodPirate

Deploy changes to the server and/or AI worker.

**Argument:** $ARGUMENTS (one of: `all`, `server`, `worker` — default: `all`)

## Steps

### 1. Commit & push (if needed)
- Check `git status` for uncommitted changes
- If there are changes, commit and push to `main`
- If clean, just ensure we're up to date with remote

### 2. Deploy to Server (unless target is `worker`)
- SSH: `ssh erik@direct.erikivarsson.com -p 2222`
- Repo: `~/Projects/Web/podpirate`
- Commands:
  ```
  cd ~/Projects/Web/podpirate && git pull && docker compose up -d --build
  ```

### 3. Deploy to AI Worker (unless target is `server`)
- SSH: `ssh erik@192.168.1.245`
- Repo: `~/Projects/AI/podpirate`
- Commands:
  ```
  cd ~/Projects/AI/podpirate && git pull && docker compose -f docker-compose.worker.yml up -d --build
  ```

### 4. Verify
- After server deploy: `ssh erik@direct.erikivarsson.com -p 2222 'docker compose -C ~/Projects/Web/podpirate ps'`
- After worker deploy: `ssh erik@192.168.1.245 'docker compose -f ~/Projects/AI/podpirate/docker-compose.worker.yml ps'`

## Notes
- Server runs: postgres, backend, web (docker-compose.yml)
- Worker runs: whisper-service (GPU), ollama (GPU) (docker-compose.worker.yml)
- The `--build` flag ensures images are rebuilt with code changes
- Worker rebuilds can take a while due to the whisper-service GPU Dockerfile
