# Contributing to AhSilence

Thank you for your interest in contributing! This guide will help you get started.

## Getting Started

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create a branch** from `main` using the naming convention below
4. **Make your changes**, following the architectural rules
5. **Open a Pull Request** against `main`

## Branch Naming

```
feature/short-description
fix/short-description
docs/short-description
refactor/short-description
```

## Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org):

```
feat: add adaptive frequency tracking
fix: resolve phase drift in emission loop
docs: update architecture diagram
refactor: extract constants from NativeAudioDSP
```

## Architectural Rules

These are **non-negotiable** for any contribution:

### 1. Zero Allocations in Hot Loops

Any modifications to `NativeAudioDSP.kt` or `FastFourier.kt` **must not**:
- Use the `new` keyword or create objects inside the emission/FFT loop
- Use `String` concatenation, `List` builders, or boxed types
- Trigger the JVM Garbage Collector

### 2. Domain Isolation

The `domain/` package must contain **zero Android imports**. No `Context`, `Intent`, `Bundle`, or Android lifecycle references may leak into `domain/model/` or `domain/port/`.

### 3. UI Purity

All Jetpack Compose screens must remain **stateless**. State is hoisted via parameters — never stored locally in a `@Composable` function (except transient UI-only state like toggle visibility).

### 4. Theme Consistency

Use colors from `MaterialTheme.colorScheme` or the `Color.kt` definitions. Do not hardcode hex color values in composables.

## Development Setup

- **Android Studio** Ladybug (2024.2.1+)
- **JDK 21+**
- **Physical device** required for DSP testing (emulators lack low-latency audio hardware)

```bash
git clone https://github.com/<your-fork>/ahsilence.git
cd ahsilence
./gradlew clean assembleDebug
```

## Pull Request Checklist

- [ ] Code compiles without errors
- [ ] No new Android imports in `domain/`
- [ ] No object allocations in DSP hot loops
- [ ] UI changes use theme colors, not hardcoded hex
- [ ] Commit messages follow Conventional Commits

## Questions?

Open a [Discussion](https://github.com/ahmadhassan-bted/ahsilence/discussions) or an Issue.
