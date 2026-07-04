## Description

<!-- What does this PR do? Why is it needed? -->

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor
- [ ] Documentation
- [ ] CI/Build

## Checklist

- [ ] Code compiles without errors (`./gradlew assembleDebug`)
- [ ] No new Android imports in `domain/` package
- [ ] No object allocations in DSP hot loops (`NativeAudioDSP`, `FastFourier`)
- [ ] UI changes use theme colors from `Color.kt`, not hardcoded hex values
- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org)
