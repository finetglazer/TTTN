# 1. When deploy, let's deploy no cache because of changing old code.
```bash
docker-compose up --build --force-recreate --remove-orphans -d 
```

# 2. If 