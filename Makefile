.PHONY: help up down logs test verify run clean rebuild

MVN ?= ./mvnw

help:
	@echo "Targets:"
	@echo "  up       - Start Postgres container in background"
	@echo "  down     - Stop Postgres container"
	@echo "  logs     - Tail Postgres logs"
	@echo "  test     - Run unit tests (mvn test)"
	@echo "  verify   - Run full build with integration + arch tests"
	@echo "  run      - Start the Spring Boot application (app module)"
	@echo "  clean    - Remove build artifacts"
	@echo "  rebuild  - clean + verify"

up:
	docker compose up -d postgres

down:
	docker compose down

logs:
	docker compose logs -f postgres

test:
	$(MVN) -B test

verify:
	$(MVN) -B verify

run:
	$(MVN) -pl app -am spring-boot:run

clean:
	$(MVN) -B clean

rebuild: clean verify
