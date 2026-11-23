# Bale Support Assistant — MVP 0.0.1

A runnable Spring Boot starter for the Bale support assistant concept.

## Current scope

- Persian RTL dashboard using Thymeleaf, HTML, CSS and JavaScript
- Mock ticket list and mock conversation history
- REST endpoints for listing and reading tickets
- Mock draft generation based on ticket ID
- Editing and saving a draft in application memory

## Not implemented yet

- No connection to `web.bale.ai`
- No Playwright/browser automation
- No official Bale API integration
- No Spring AI / LLM / RAG
- No persistent database yet (H2 is included for the next implementation step, but current demo data and drafts are in-memory)
- No message sending

The UI clearly marks the current integration and AI modes as `MOCK`.

## Requirements

- JDK 21
- IntelliJ IDEA
- Maven (or IntelliJ's bundled Maven)

## Run in IntelliJ

1. Extract the ZIP.
2. IntelliJ IDEA → **Open** → select the extracted `bale-support-assistant` folder.
3. Allow IntelliJ to import the Maven project.
4. Set Project SDK to **Java 21**.
5. Open `BaleSupportApplication.java`.
6. Run `main()`.

Or from a terminal:

```bash
mvn spring-boot:run
```

Open:

- Dashboard: http://localhost:8080
- Tickets API: http://localhost:8080/api/v1/tickets
- One ticket: http://localhost:8080/api/v1/tickets/BAL-1001

## API examples

Generate a draft:

```bash
curl -X POST http://localhost:8080/api/v1/drafts/generate ^
  -H "Content-Type: application/json" ^
  -d "{\"ticketId\":\"BAL-1001\"}"
```

Update a draft:

```bash
curl -X PUT http://localhost:8080/api/v1/drafts/BAL-1001 ^
  -H "Content-Type: application/json" ^
  -d "{\"content\":\"متن اصلاح‌شده پاسخ\"}"
```

## Next implementation steps

1. Replace in-memory ticket data with persistence.
2. Verify available and permitted Bale integration options.
3. Implement the Bale API and browser adapters behind ports.
4. Add Spring AI and a knowledge retrieval pipeline.
5. Add durable draft versions and safe draft placement in the Bale composer.
