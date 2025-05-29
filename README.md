# TeleSim

**TeleSim** is a handy little tool I built to forward SMS messages and phone calls, mostly OTPs, government and bank
alerts, from Android phones to Telegram.

When you're doing business abroad, you eventually run out of SIM slots or eSIM capacity on your phone. And swapping
eSIMs is pain in the ass as some providers take hours to activate when you're roaming.

I had a few spare Android phones lying around, plugged in and connected to Wi-Fi, so I figured: why not turn them into
local SMS forwarders?

⚠️ It’s not the most secure solution, but neither are OTPs via SMS. Oh well...

## Setup Guide

Here’s how to get it working:

1. Create a bot via [@BotFather](https://telegram.me/BotFather).
2. Copy the bot token it gives you.
3. Create one or more private Telegram channels, add the bot as an admin, and send a quick `/start` message.
4. Alternatively, just send a message to the bot directly - alerts will go to your DMs.
5. Visit `https://api.telegram.org/bot[BOT_TOKEN]/getUpdates` and grab the numeric chat ID (something like
   `-1002614630000`).
6. Open the app on your Android phone, grant all requested permissions, and enter your bot token and chat ID for each
   SIM.
7. Done! You don’t need to keep the app running - it uses broadcast receivers. Just make sure battery optimization is
   disabled in Settings.

## Building the APK

1. Clone the repository.
2. Open the project in Android Studio.
3. Build the APK: `./gradlew assembleDebug`.
4. The APK will be generated in `app/build/outputs/apk/debug/`.

Alternatively, you can build a release version: `./gradlew assembleRelease`.

## Requirements

- Android Studio
- Android SDK
- Gradle