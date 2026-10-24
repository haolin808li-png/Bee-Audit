# Local workbench deployment

> **Public test preview only. Not for operational use or public-service hosting.**
> Read [the test disclaimer](../DISCLAIMER.md) and [privacy notice](../PRIVACY.md).

This serves the English frontend and its browser-local Agent simulator. It does
not create an Agent backend, connect model APIs or upload evidence. Nothing has
been deployed to a cloud account.

## Without Docker

From the repository root, with Node.js 24 installed:

```sh
cd workbench
npm ci
npm run dev -- --port 4173 --strictPort
```

Open http://127.0.0.1:4173. This command works on macOS and Windows with Node.js
installed. No Python environment is needed for this frontend.

## Optional container

Install and start Docker with Compose support, then from the repository root:

```sh
docker compose -f deploy/compose.yaml up --build -d
docker compose -f deploy/compose.yaml logs --tail 50
```

Open http://127.0.0.1:8080. The port is restricted to this computer. Stop it with:

```sh
docker compose -f deploy/compose.yaml down
```

No cloud account, secrets or credentials are required. The Docker configuration
is a packaging recipe; see the root README for whether a container build was
actually tested on the handoff machine.

Browser data belongs to its origin. Data imported on port 4173 will not appear
on port 8080. Re-import original ZIPs when changing ports or browsers. Keep
independent backups; neither container storage nor this frontend backs up the
browser's inspection records.

Do not expose this preview to the public internet as a production service.
There is no authentication or server-side access control.

## Agent backend

See [AGENT_DEPLOYMENT.md](AGENT_DEPLOYMENT.md) for the future backend architecture,
missing implementation and production acceptance checklist.
