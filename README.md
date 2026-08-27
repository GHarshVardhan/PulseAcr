# Pulse ACR

Automatic call recorder for Android, inspired by the Cube ACR + Helper product shape.

## Modules

- `:app` — **Pulse ACR** library UI (Compose), settings, phone-state auto-record, local Room database
- `:helper` — **Pulse ACR Helper** Accessibility + phone-state companion for stronger call detection

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room + DataStore
- Foreground microphone services

## Build

Open in Android Studio, or:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug :helper:assembleDebug
```

Install both APKs. Enable the Helper Accessibility service, grant mic/phone/notification permissions, and disable battery optimization for reliable background recording.

## Legal

Call recording laws differ by jurisdiction. Only record when you have a lawful right to do so, and notify other parties when required. Remote-party audio quality on modern Android is device-dependent; third-party apps cannot reliably capture full call uplink/downlink on all OEMs.
