# Agent roadmap

Status: proposals only. None of the AI capabilities below is implemented or
deployed. The current Agent is a deterministic local simulator.

## Priority 1: evidence-grounded drafts

1. **Timestamped transcription.** Associate each transcript segment with the
   original audio, hive and visit. Flag uncertainty; retain editable corrections.
2. **Evidence-linked claims.** Every finding points to a photo, form field or
   audio timestamp. Separate observed facts, auditor statements and inference.
3. **Missing-evidence assistant.** Identify incomplete records and prepare a
   follow-up checklist. Never invent missing measurements or claim coverage.
4. **Contradiction review.** Flag disagreements between the form, notes and
   transcript for a human. Preserve both sources instead of silently choosing.
5. **Structured report drafting.** Produce a versioned draft from a fixed schema;
   validate it and require human review before any final export or communication.

## Priority 2: continuity and operator control

6. **Repeat-visit comparison.** Compare the same verified hive/location across
   visits. Show unmatched identities, changed conditions and missing evidence;
   do not treat different hives or sampling conditions as equivalent.
7. **Evidence search and questions.** Answer questions about the authorized visit
   collection with citations and an explicit "not enough evidence" outcome.
8. **Photo-quality checks.** Flag blur, darkness, duplicates and irrelevant shots.
   Quality feedback is not colony-health diagnosis.
9. **Cost-aware routing.** Route appropriate tasks to cheaper or stronger providers
   using evaluated results, budgets, retries and per-job usage records. Do not
   select providers on price alone or transmit data without consent.
10. **Draft-to-review handoff.** Support reviewer corrections, revision history,
    explicit approval and report regeneration without overwriting source evidence.

## Priority 3: organization-scale operations

- Orchard-level completeness summaries and assigned follow-up tasks.
- De-identified cross-visit statistics with clear denominators and missingness.
- Configurable assessment templates, reviewed by domain experts.
- Durable background workers, checkpoints and recovery from interrupted uploads.
- Organization-scoped access, audit logs, retention and tested restore procedures.

## Guardrails before real use

Private storage; per-organization permissions; provenance for every claim;
untrusted-input handling; consent before provider uploads; no credentials in
clients; spend caps; model/version tracking; evaluation on consented examples;
no autonomous certification, billing decisions, data deletion or external sending.

Recommended first vertical slice: one synthetic visit ZIP -> transcript with
timestamps -> evidence-linked findings -> unsigned draft -> human review. Compare
against a human-authored reference before expanding the scope.
