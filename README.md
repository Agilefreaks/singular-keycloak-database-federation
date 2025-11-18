# singular-keycloak-database-federation

### Compatible with Keycloak 26.4.

## Build

```bash
  docker compose up
```

## Add to keycloak
### Dockerfile
Add the .jar package before building Keycloak.

```Dockerfile
# ...
ADD --chown=keycloak:keycloak --chmod=644 \
    https://github.com/pdany1116/singular-keycloak-database-federation/raw/main/dist/singular-user-storage-provider-with-dependencies.jar \
    /opt/keycloak/providers/
#...
```

### Local
1) Copy every `singular-user-storage-provider-with-dependencies.jar` to /providers.
2) Build and start keycloak:
```bash
   ./bin/kc.sh start-dev
```

