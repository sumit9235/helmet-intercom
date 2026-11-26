# Helmet Intercom Lab — Phase 1
Native Android Java diagnostic (Android 12+). No server, network permission or stored audio. This is not yet an intercom. Java was chosen to minimize build dependencies.

## Build
Upload the contents of this folder (including hidden `.github`) to a GitHub repository. Open Actions > Build diagnostic APK > Run workflow, or push to trigger it. Download the `helmet-intercom-diagnostic` artifact, extract the APK and install on your phone. The project has not been compiled here because Android SDK/Gradle are unavailable.

## Test while stationary, at low volume
1. Pair both headsets in Android Bluetooth settings, enable calls and media where available. Grant microphone and nearby-device permissions.
2. Refresh routes. If only one appears, switch the active headset in system settings, refresh again and test each independently. Do not interpret a paired device as an active audio endpoint.
3. Select A and B. Test A and B separately: speak during each test; verify RMS changes and hear the short tone in the intended headset.
4. Run dual microphones: speak into A alone, then B alone, then both. Compare actual routed device IDs and RMS. Distinct IDs and independent signals are required. Zero RMS or same ID is not a pass. Address matching may be unavailable, in which case the app reports inconclusive rather than guessing.
5. Run dual speakers. A should hear 440 Hz and B 660 Hz alternating at roughly one-second intervals. Verify physically and inspect routes. Timing can vary. Stop if uncomfortable.
6. Copy diagnostic log and share it after redacting Bluetooth addresses if desired.

The app requests one communication route and per-stream preferences; Android may override those. This prototype does not prove all four streams can coexist, does not forward mic audio, and stops testing when backgrounded. A later phase can attempt the four-stream test only if diagnostics support it. No root required.
