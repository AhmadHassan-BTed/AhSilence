# Support

## Getting Help

- **Bug Reports**: [Open an issue](https://github.com/ahmadhassan-bted/ahsilence/issues/new?template=bug_report.yml)
- **Feature Requests**: [Open an issue](https://github.com/ahmadhassan-bted/ahsilence/issues/new?template=feature_request.yml)
- **Discussions**: [GitHub Discussions](https://github.com/ahmadhassan-bted/ahsilence/discussions)

## FAQ

**Q: Why does the app need microphone permission?**
A: AhSilence captures a 7-second ambient audio sample to detect the dominant low-frequency hum (e.g., AC units). Audio is processed in-memory and never recorded or transmitted.

**Q: Does it work with Bluetooth headphones?**
A: Yes, but Bluetooth introduces latency. Use the Phase Calibrator dial to manually compensate for the delay.

**Q: Why can't I use an emulator for testing?**
A: Android emulators do not accurately simulate low-latency hardware microphone and audio output, which are critical for FFT analysis and real-time wave emission.

**Q: The cancellation isn't perfect. What can I do?**
A: Enable PRO mode and fine-tune the Phase Shift dial. Small adjustments (±5°) around 180° can significantly improve cancellation depending on your hardware and environment.
