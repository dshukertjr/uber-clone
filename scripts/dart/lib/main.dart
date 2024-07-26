import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:supabase/supabase.dart';

const supabaseUrl = 'https://szooyrexjzmsnjuprczd.supabase.co';
const supabaseKey =
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN6b295cmV4anptc25qdXByY3pkIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTcyMTgxNTY3MCwiZXhwIjoyMDM3MzkxNjcwfQ.LDv7t7Yq7BI9RxZ0_i9lhBEQwbIMXXQEKwK7K1Yu19c';

const googleMapsApiKey =
    'AIzaSyCXohGOxjuDI0U-AGE2i1AHNeyKJDEIkmw';

// Replace with an actual ride ID from your database
const rideId = '0649f0b1-8018-4a21-9931-e17a93728506';

void main() async {
  final supabase = SupabaseClient(supabaseUrl, supabaseKey);

  // Fetch ride details and driver location using the custom Postgres function
  final response = await supabase.rpc('get_ride_and_driver',
      params: {'ride_id': rideId}).single();

  print(response);

  final driverId = response['driver_id'];
  final origin = response['origin'];
  final destination = response['destination'];
  final driverLocation = response['driver_location'];

  // Get route from driver to pickup location
  final pickupRoute = await getRoute(
      origin: driverLocation, destination: origin);

  // Simulate driver picking up passenger
  await simulateDriving(
    supabase: supabase,
    driverId: driverId,
    route: pickupRoute,
    // durationInSeconds: 10,
  );

  // Update ride status to 'riding'
  await supabase
      .from('rides')
      .update({'status': 'riding'}).eq('id', rideId);

  // Get route from pickup to destination
  final rideRoute =
      await getRoute(origin: origin, destination: destination);

  // Simulate ride to destination
  await simulateDriving(
    supabase: supabase,
    driverId: driverId,
    route: rideRoute,
    // durationInSeconds: 20,
  );

  // Update ride status to 'completed'
  await supabase
      .from('rides')
      .update({'status': 'completed'}).eq('id', rideId);

  print('Ride simulation completed');
}

// Helper function to get route from Google Maps API
Future<List<Map<String, double>>> getRoute({
  required Map<String, dynamic> origin,
  required Map<String, dynamic> destination,
}) async {
  final url = Uri.parse(
      'https://routes.googleapis.com/directions/v2:computeRoutes?key=$googleMapsApiKey');

  final response = await http.post(url,
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-FieldMask':
            'routes.duration,routes.distanceMeters,routes.polyline,routes.legs.polyline',
      },
      body: jsonEncode({
        'origin': {
          'location': {
            'latLng': {
              'latitude': origin['lat'],
              'longitude': origin['lng']
            }
          }
        },
        'destination': {
          'location': {
            'latLng': {
              'latitude': destination['lat'],
              'longitude': destination['lng']
            }
          }
        },
        'travelMode': 'DRIVE',
        'polylineEncoding': 'GEO_JSON_LINESTRING',
      }));

  if (response.statusCode == 200) {
    final data = json.decode(response.body);
    final steps = data['routes'][0]['legs'][0]['polyline']
        ['geoJsonLinestring']['coordinates'] as List<dynamic>;

    return steps.map<Map<String, double>>((step) {
      return {
        'lat': step[1],
        'lng': step[0],
      };
    }).toList();
  } else {
    final data = json.decode(response.body);
    print(data);

    throw Exception('Failed to get route');
  }
}

// Helper function to simulate driving
Future<void> simulateDriving({
  required SupabaseClient supabase,
  required String driverId,
  required List<Map<String, double>> route,
}) async {
  for (var i = 0; i < route.length; i++) {
    final location = route[i];
    await supabase.from('drivers').update({
      'location': 'POINT(${location['lng']} ${location['lat']})',
    }).eq('id', driverId);
    await Future.delayed(Duration(milliseconds: 300));
  }
}
