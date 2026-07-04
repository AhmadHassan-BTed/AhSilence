# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability, **please do not open a public issue**.

Instead, report it privately by emailing the maintainer or using GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability).

We will acknowledge receipt within 48 hours and provide a timeline for a fix.

## Security Considerations

- **No network calls**: AhSilence is a fully offline, client-side application. It makes zero network requests.
- **No data collection**: No telemetry, analytics, or user data is transmitted.
- **Microphone access**: The app requires `RECORD_AUDIO` permission solely for ambient noise analysis. Audio data is processed in-memory and never stored or transmitted.
- **Foreground Service**: Used exclusively to prevent the OS from killing the DSP loop. No background data processing occurs.
