# Uber driver simulation script

This script simulates the Uber driver by sending the driver's location to the Supabase database. The Flutter app can then listen to the driver's location and update the driver's location on the map.

## Setup the environment

Open `scripts/dart/lib/main.dart` and replace `YOUR_SUPABASE_URL` and `YOUR_SERVICE_ROLE_KEY` with your own Supabase URL and service role key.

## Usage

From the Flutter app, choose a destination and confirm the fare. This will find a nearby driver, and insert a new row into the `rides` table. Identify the `ride` associated to the app, and copy and paste the `ride_id` into `TARGET_RIDE_ID` in `scripts/dart/lib/main.dart`.

Once the `TARGET_RIDE_ID` is set, run the script with `dart run lib/main.dart`. The script will then simulate the driver by sending the driver's location to the Supabase database.
