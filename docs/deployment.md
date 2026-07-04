# Deployment

## Debug Build

```bash
./gradlew assembleDebug
# APK location: app/build/outputs/apk/debug/app-debug.apk
```

## Release Build

### 1. Create a Signing Key

```bash
keytool -genkey -v -keystore ahsilence-release.jks \
  -alias ahsilence -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Configure Signing

Create `keystore.properties` in the project root (already in `.gitignore`):

```properties
storeFile=../ahsilence-release.jks
storePassword=your_password
keyAlias=ahsilence
keyPassword=your_password
```

### 3. Build Signed APK

```bash
./gradlew assembleRelease
# APK location: app/build/outputs/apk/release/app-release.apk
```

## CI/CD

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push and PR to `main`:

1. **Lint** — `./gradlew lint`
2. **Build** — `./gradlew assembleDebug`
3. **Test** — `./gradlew test`

Debug APKs are uploaded as artifacts on push to `main`.

## Play Store Considerations

- **Microphone permission**: Google Play requires a privacy policy explaining why `RECORD_AUDIO` is used
- **Foreground service**: Must declare `foregroundServiceType="mediaPlayback"` in the manifest (already configured)
- **Target SDK**: Currently targeting SDK 35 for compatibility; update as Google Play requirements evolve
