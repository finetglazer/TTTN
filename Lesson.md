# 1. When deploy, let's deploy no cache because of changing old code.
```bash
docker-compose up --build --force-recreate --remove-orphans
```

# 2. If 