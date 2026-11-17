# Morning Quest — Native Android

This is a native Android application built with Kotlin and Jetpack Compose. It does not load the original HTML or use a WebView/PWA.

## Included
- Dark ember/amber visual style inspired by the supplied Morning Quest design
- Today routine, task creation/deletion/completion, time and duration fields, routine templates
- XP, levels, coins, streaks, combo bonus, completion reward, badges
- Daily boss progress and random loot box rewards
- Growing plant/pet companion with daily care limit
- Theme selector, custom rewards and reward claiming
- Seven-day report and skipped-task summary
- Local on-device persistence

## Build APK on GitHub
1. Create a GitHub repository.
2. Upload the contents of this folder (not the folder wrapper) to the repository root.
3. Open **Actions** and run **Build native Morning Quest APK** (or push to `main`).
4. Download the `Morning-Quest-Native-APK` artifact. Inside is `app-debug.apk`.
5. Transfer it to your Android phone and install it.

## Important scope notes
- The original leaderboard entries are sample players; there is no online account or multiplayer backend.
- The original ₹99 Premium purchase was a placeholder. Google Play Billing is not configured, and this build will not charge money or falsely unlock paid features.
- Reminder time is stored, but scheduled background notifications are not yet wired up in this initial native build.
- This is a debug APK build workflow. A Play Store release requires a signing key and release configuration.
