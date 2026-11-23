# Bale Bot - Spring AI draft integration

## Files
Copy the ZIP contents into the project root and overwrite matching files.

## Enable Spring AI (PowerShell)
Set these environment variables in the same terminal used to start Spring Boot:

```powershell
$env:OPENAI_API_KEY = "YOUR_API_KEY"
$env:SPRING_AI_MODEL_CHAT = "openai"
$env:OPENAI_MODEL = "gpt-4o-mini"
mvn spring-boot:run
```

For IntelliJ, add the same variables to Run/Debug Configuration > Environment variables.

Without `SPRING_AI_MODEL_CHAT=openai`, the application remains startable, but the AI endpoint returns HTTP 503 and the UI shows the configuration message.

## Endpoint
`POST /api/v1/ai/drafts/generate`

```json
{
  "conversationUid": "UID_OF_SELECTED_BALE_CHAT",
  "tone": "MATCH_USER"
}
```

Tone values: `MATCH_USER`, `FORMAL`, `FRIENDLY`.

## Current scope and safeguards
- Uses Spring AI ChatClient with the OpenAI chat model.
- Checks that the UID currently open in the Playwright-controlled Bale page matches the UID selected in the UI.
- Builds a style prompt from up to 8 outgoing text messages in the selected conversation.
- Builds up to 5 question/answer examples only where incoming and outgoing directions are explicitly detected.
- Includes up to 24 recent messages as context.
- Does not send the generated answer to Bale.
- If the latest text message is direction-unknown, the response includes a warning and requires operator review.
- Style examples are currently taken from the selected conversation only. A cross-conversation style profile and searchable historical Q&A knowledge base require persisting historical messages and are a subsequent step.
