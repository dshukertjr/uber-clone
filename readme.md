# Uber Clone with Supabase

A collection of Uber clone apps build with Supabase and different frameworks.

- flutter: Uber clone app build with Flutter
- android: [WIP] Uber clone app build with Android Jetpack Compsoe
- script: Contains scripts that simulates the Uber driver

Follow the video guide to build the app: https://youtu.be/cL4pVpaOH9o

## Setup the environment

### Link Supabase project

Run `supabase link` to link the project to a remote Supabase project. Once linked, run `supabase secrets set --env-file ./supabase/functions/.env` to set the environment variables.

### Database migrations

Run `supabase db push` to push the migrations to the Supabase database.

### Edge functions

Copy the `.env.example` file to `.env` and paste your own Google Cloud Platform API key. Make sure the Routes API is enabled for the key.

### Flutter

- Open `flutter/lib/main.dart` and replace the `SUPABASE_URL` and `SUPABASE_ANON_KEY` in the `Supabase.initialize()` call with your own Supabase URL and anon key.
- Opne `flutter/android/app/src/main/AndroidManifest.xml` and replace `YOUR_GOOGLE_MAP_API_KEY` with your own Google Maps API key.
- Open `flutter/ios/Runner/AppDelegate.swift` and replace `YOUR_GOOGLE_MAP_API_KEY` with your own Google Maps API key.

### Dart Script

- Open `scripts/dart/lib/main.dart` and replace `YOUR_SUPABASE_URL` and `YOUR_SERVICE_ROLE_KEY` with your own Supabase URL and service role key.
- In the same file, replace `YOUR_GOOGLE_ROUTES_API_KEY` with your own Google Cloud Platform API key with the routes API enabled.
- Also once the user finds a driver and a ride is created, update the `TARGET_RIDE_ID` to the target ride ID to simulate a driver picking up the customer and driving to the destination.

### Dummy driver data

In order for the app to find an available driver, there needs to be a driver in the database. You can add a driver to the database by running the following SQL query:

```sql
insert into public.drivers (id, model, number, location, is_available)
    values
    ('a040ae05-3928-4cbf-8577-bbad6125c3fe', 'Ford Focus', 'GHI-789', ST_GeographyFromText('SRID=4326;POINT(-122.0854 37.4223983)'), true);
```
