# Security Policy

## Supported status

Only the current public-test branch is maintained on a best-effort basis.
There is no production-supported version, patch SLA or bug-bounty commitment.
The bundled debug APK is a historical test artifact, not a hardened release.

## Private reporting

Use [Report a vulnerability](https://github.com/haolin808li-png/Bee-Audit/security/advisories/new)
to submit a private GitHub vulnerability report. Include the commit/build,
affected component, impact and minimal synthetic reproduction. Do not upload
real inspection archives, credentials, personal information or private locations.

If the private reporting option is unavailable, open a public issue asking for
a private contact channel **without** technical exploit details or sensitive data.
Do not disclose a vulnerability in an ordinary public issue before coordination.
See [GitHub's private reporting guide](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/report-privately).

## Safe research boundaries

Test only your own local instance and synthetic data. Do not access others'
devices, accounts or inspection records. Do not test availability attacks against
third-party services. No authority to test any external system is granted here.

## Known limitations

- No server-side authentication, authorization, tenant isolation or cloud backup.
- Browser-local roles and review labels are not security controls.
- Local ZIP validation is not a substitute for a secure server import pipeline.
- No application-level encryption, immutable audit trail or signed reports.
- Future AI inputs must be treated as untrusted evidence, not executable instructions.
- Never place model credentials in the browser bundle or Android APK.

If a credential is exposed, revoke/rotate it immediately; deleting a file or
commit alone is not sufficient remediation. Report accidental personal-data
exposure privately and avoid making further copies.
