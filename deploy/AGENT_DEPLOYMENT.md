# Agent deployment handoff

## What exists today

- `workbench/src/workflow.js`: deterministic browser-local simulation state machine.
- `workbench/src/main.js`: run controls, simulation logs, review gate and report UI.
- `workbench/src/data.js`: local rule flags and evidence-based report template.
- `deploy/`: container packaging for the frontend only.

There is no Python service, LangGraph graph, API server, worker, queue, speech
transcription or real model integration in this repository. The DeepSeek and
OpenAI choices are labels in simulation preferences, not API integrations. Do
not describe this package as a deployed or production-ready Agent.

## Proposed backend architecture (not implemented)

```text
Android exports / browser upload
  -> authenticated API
  -> private object storage + relational database
  -> durable job queue
  -> Agent worker
     -> validate manifest and evidence
     -> transcribe voice notes
     -> structure observations (candidate: DeepSeek API)
     -> review complex findings (candidate: OpenAI API)
     -> validate citations and unresolved evidence gaps
     -> prepare a draft
  -> human reviewer
  -> versioned report export
```

LangGraph is a candidate for durable orchestration and checkpoints. Provider
model IDs, speech service, hosting region, consent and cost limits must be
selected and tested before implementing integrations. A ChatGPT subscription is
not an API credential or an API billing arrangement.

## Suggested interface contract

These routes are a proposal, not live endpoints:

- `POST /visits`: receive a manifest and return an ID and upload instructions.
- `POST /visits/{id}/complete`: verify checksums and finalize evidence receipt.
- `POST /runs`: create a job with visit ID, configuration and idempotency key.
- `GET /runs/{id}`: return server-owned state, completed stages and safe logs.
- `POST /runs/{id}/cancel`: request cooperative cancellation.
- `POST /runs/{id}/retry`: retry eligible failures from a saved checkpoint.
- `GET /reports/{id}`: retrieve a versioned draft with source references.
- `POST /reports/{id}/review`: record authenticated human review.

Do not trust client-provided review status, owner, source provenance or model
results. Enforce organization boundaries and role permissions on every route.

## Implementation and rollout checklist

- [ ] Choose hosting region, retention, backup and data-processing consent.
- [ ] Implement authentication and organization-scoped authorization.
- [ ] Validate archives again on the server; bound compressed/expanded sizes,
      reject traversal, malformed records and invalid GPS coordinates.
- [ ] Store originals privately with checksums and immutable evidence IDs.
- [ ] Add durable jobs, idempotency, timeouts, retry limits and checkpoints.
- [ ] Keep all provider keys in server-side secrets, never browser or APK.
- [ ] Add transcription with per-audio-file provenance and uncertainty labels.
- [ ] Implement provider adapters, explicit model configuration and budget caps.
- [ ] Treat imported notes/transcripts as untrusted data, not instructions.
- [ ] Require report claims to reference evidence; do not infer unseen health
      conditions or invent measurements. Preserve unknowns and conflicts.
- [ ] Separate template output, model inference and human judgement in reports.
- [ ] Add human review, report versioning, audit history and signed downloads.
- [ ] Replace frontend simulation with server status polling or events.
- [ ] Test tenant isolation, interrupted uploads, duplicate jobs, provider
      outages, cancellation, backup recovery and realistic field data.
- [ ] Deploy to staging, then authorize production rollout separately.

No deployment command for a real Agent is provided because no such backend has
been implemented yet.
