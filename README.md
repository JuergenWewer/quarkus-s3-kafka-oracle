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

## Lokaler Start des Tests
```bash
mvn clean install
```

                           ┌────────────────────────────────────────────┐
                           │        END-TO-END PIPELINE (Ablauf)        │
                           └────────────────────────────────────────────┘

┌──────────────┐        ┌────────────────────┐        ┌──────────────────────┐
│   TESTCODE   │        │    KAFKA (in)      │        │      QUARKUS APP     │
│  (JUnit)     │ -----> │  Topic: names-in   │ -----> │  consumes JSON       │
└──────────────┘        └────────────────────┘        │  validates message   │ 
                                                      │                      │
                                                      │   ┌─────────────────────────────┐
                                                      │   │  Save JSON to S3 (Localstack)│
                                                      │   └─────────────────────────────┘
                                                      │                      │
                                                      │   ┌─────────────────────────────┐
                                                      │   │ Insert name into Oracle XE  │
                                                      │   │ SELECT MAX(id) to get new ID│
                                                      │   └─────────────────────────────┘
                                                      │                      │
                                                      │   ┌─────────────────────────────┐
                                                      │   │ Produce ID to Kafka (names-out)│
                                                      │   └─────────────────────────────┘
                                                      │
                                                      └──────────────┬────────
                                                                     │                                                      
                                                                     ▼
                                                      ┌────────────────────────────────────┐
                                                      │      KAFKA (out)                   │
                                                      │   Topic: names-out                 │
                                                      └────────────────────────────────────┘
                                                                     │
                                                                     ▼
                                                      ┌───────────────────────────┐
                                                      │ TESTCODE liest ID zurück  │
                                                      │ → Assertion erfolgreich   │
                                                      └───────────────────────────┘


            ┌─────────────────────────────────────────────────────────────────────────┐
            │                 TESTCONTAINER-INFRASTRUKTUR (Übersicht)                 │
            └─────────────────────────────────────────────────────────────────────────┘

                        ┌─────────────────────────────────────────────────┐
                        │                   TEST JVM                      │
                        │  JUnit + QuarkusTest orchestrieren alles        │
                        └─────────────────────────────────────────────────┘
                                         │
                ┌────────────────────────┼─────────────────────────┐
                ▼                        ▼                         ▼
      ┌────────────────┐       ┌────────────────┐        ┌─────────────────────┐
      │   KAFKA        │       │  LOCALSTACK    │        │    ORACLE XE        │
      │ Testcontainer  │       │ S3 Testcontainer│       │  Testcontainer      │
      └────────────────┘       └────────────────┘        └─────────────────────┘
                ▲                        ▲                         ▲
                └───────────────┬────────┴───────────┬─────────────┘
                                ▼                    ▼
                           ┌─────────────────────────────────────────┐
                           │         QUARKUS (System under Test)     │
                           │   → konsumiert JSON                     │
                           │   → schreibt S3                         │
                           │   → schreibt Oracle DB                  │
                           │   → sendet ID an Kafka                  │
                           └─────────────────────────────────────────┘



