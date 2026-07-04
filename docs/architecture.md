# Architecture

AhSilence follows **Clean Architecture** (MVVM variant) with three strict layers. Dependencies flow inward — outer layers depend on inner layers, never the reverse.

## Layer Boundaries

```
┌─────────────────────────────────────┐
│            UI Layer                 │  Compose screens, components, theme
│         (ui/, MainActivity)        │  Knows about: Presentation
├─────────────────────────────────────┤
│        Presentation Layer           │  ViewModel, StateFlow
│         (presentation/)            │  Knows about: Domain, Core
├─────────────────────────────────────┤
│          Domain Layer               │  Pure Kotlin models & interfaces
│          (domain/)                 │  Knows about: Nothing (zero imports)
├─────────────────────────────────────┤
│           Data Layer                │  Android SDK implementations
│          (data/)                   │  Knows about: Domain, Core
├─────────────────────────────────────┤
│           Core Layer                │  Constants, DI, Logging
│          (core/)                   │  Knows about: Domain (for DI wiring)
└─────────────────────────────────────┘
```

## Domain Layer (`domain/`)

**Zero Android dependencies.** This package contains only pure Kotlin.

- `model/AcousticState.kt` — Immutable data class representing the entire app state (detected frequency, amplitude, phase, emission status)
- `port/AudioEngine.kt` — Interface contract for the DSP engine. The ViewModel and Service depend on this abstraction, never on the concrete implementation

## Data Layer (`data/`)

**Heavy OS / hardware integrations.** Implements the domain contracts using Android APIs.

- `engine/FastFourier.kt` — Zero-allocation Cooley-Tukey Radix-2 FFT with pre-computed trig tables
- `engine/NativeAudioDSP.kt` — `AudioRecord` capture + `AudioTrack` emission pipeline implementing `AudioEngine`
- `service/ActiveHumService.kt` — Android Foreground Service that prevents the OS from killing the DSP loop

## Presentation Layer (`presentation/`)

- `ControlViewModel.kt` — Bridges domain state to the UI via `StateFlow`. Triggers environment analysis on init, forwards parameter changes to the engine

## UI Layer (`ui/`)

- `screen/DashboardScreen.kt` — Stateless Compose screen with Pro/Simple mode toggle
- `component/CalibratorRing.kt` — Custom Canvas-based 360° phase dial
- `component/GainSlider.kt` — Custom Canvas-based vertical amplitude fader
- `theme/` — OLED Black / Neon Amber Material 3 design system

## Core Layer (`core/`)

Cross-cutting concerns shared across all layers.

- `constants/AudioConstants.kt` — All magic numbers centralised (sample rate, buffer size, notification IDs, intent actions)
- `di/AudioEngineLocator.kt` — Lightweight service locator ensuring the ViewModel and Service share the same engine instance
- `logging/AppLogger.kt` — Thin facade over `android.util.Log` for consistent tagging

## Key Design Decisions

1. **No DI framework** — A singleton service locator is sufficient for a single-module app with one shared dependency. Hilt/Koin would add complexity without benefit.

2. **@Volatile over synchronization** — The DSP emission loop reads parameters via `@Volatile` fields written by the UI thread. This is lock-free, GC-free, and sufficient for primitive types.

3. **Pre-allocated memory pools** — `FastFourier` pre-allocates all arrays and trig tables at construction time. The `extractDominantFrequency()` method creates zero objects per invocation.

4. **Foreground Service over WorkManager** — The audio emission loop must run with real-time priority and zero latency. WorkManager is designed for deferrable work and would introduce unacceptable delays.
