# Quarkus: Kafka -> S3 -> Oracle (E2E, Testcontainers, K8s)

- Liest JSON `{ "name": "..." }` von `names-in`
- Speichert JSON in S3 (LocalStack/MinIO) unter `names/<timestamp>-<uuid>.json`
- Insert in Oracle `names(id, name)` (IDENTITY)
- Schreibt die generierte `id` auf Topic `names-out`

## CI: SUSE + Testcontainers + K8s
Siehe `.github/workflows/suse-testcontainers-ci-k8s.yml` (DinD für Testcontainers, Image Build/Push, K8s Deploy).

## Secrets
- `IMAGE_REGISTRY`, `IMAGE_REPO`, `REGISTRY_USER`, `REGISTRY_PASSWORD`
- `KUBECONFIG_B64`

## Lokaler Start
```bash
mvn -DskipTests package
java -jar target/quarkus-app/quarkus-run.jar
```