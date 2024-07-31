# Uber clone with Flutter app

An Uber clone app for Android and iOS built with Flutter and Supabase. A user can find a nearby driver, confirm the fare, and track the driver's location in real-time. Once a driver is found, you can simulate a driver by running the scripts in the `scripts/dart` folder.

Watch the full video guide on how to build this app: https://youtu.be/cL4pVpaOH9o

## Setup the environment

- Open `flutter/lib/main.dart` and replace the `SUPABASE_URL` and `SUPABASE_ANON_KEY` in the `Supabase.initialize()` call with your own Supabase URL and anon key.
- Opne `flutter/android/app/src/main/AndroidManifest.xml` and replace `YOUR_GOOGLE_MAP_API_KEY` with your own Google Maps API key.
- Open `flutter/ios/Runner/AppDelegate.swift` and replace `YOUR_GOOGLE_MAP_API_KEY` with your own Google Maps API key.

## Usage

Run the app from the IDE or by running `flutter run` in the terminal.
