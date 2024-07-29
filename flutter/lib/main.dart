import 'dart:async';
import 'dart:math';

import 'package:duration/duration.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:intl/intl.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

void main() async {
  await Supabase.initialize(
    url: 'https://rmjwhnhfotnpbnjfnxka.supabase.co',
    anonKey:
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJtandobmhmb3RucGJuamZueGthIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTkzNzMzODYsImV4cCI6MjAzNDk0OTM4Nn0.l5C85uPdndHNWDQPXr8OPPBOjqCgnsn2bvhWtiTy328',
  );
  runApp(const MainApp());
}

class MainApp extends StatelessWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: UberCloneMainScreen(),
      ),
    );
  }
}

enum AppState {
  choosingLocation,
  confirmingFare,
  waitingForPickup,
  riding,
  postRide,
}

enum RideStatus {
  picking_up,
  riding,
  completed,
}

class Ride {
  final String id;
  final String driverId;
  final String passengerId;
  final int fare;
  final RideStatus status;

  Ride({
    required this.id,
    required this.driverId,
    required this.passengerId,
    required this.fare,
    required this.status,
  });

  factory Ride.fromJson(Map<String, dynamic> json) {
    return Ride(
      id: json['id'],
      driverId: json['driver_id'],
      passengerId: json['passenger_id'],
      fare: json['fare'],
      status: RideStatus.values
          .firstWhere((e) => e.toString().split('.').last == json['status']),
    );
  }
}

class Driver {
  final String id;
  final String model;
  final String number;
  final bool isAvailable;
  final LatLng location;

  Driver({
    required this.id,
    required this.model,
    required this.number,
    required this.isAvailable,
    required this.location,
  });

  factory Driver.fromJson(Map<String, dynamic> json) {
    return Driver(
      id: json['id'],
      model: json['model'],
      number: json['number'],
      isAvailable: json['is_available'],
      location: LatLng(json['latitude'], json['longitude']),
    );
  }
}

class UberCloneMainScreen extends StatefulWidget {
  const UberCloneMainScreen({super.key});

  @override
  UberCloneMainScreenState createState() => UberCloneMainScreenState();
}

class UberCloneMainScreenState extends State<UberCloneMainScreen> {
  final _supabase = Supabase.instance.client;
  AppState _appState = AppState.choosingLocation;
  late GoogleMapController _mapController;
  CameraPosition _initialPosition = const CameraPosition(
    target: LatLng(37.7749, -122.4194),
    zoom: 14.0,
  );
  LatLng? _selectedDestination;
  LatLng? _currentLocation;
  final Set<Polyline> _polylines = {};
  // VziFGVpVmqSP082n
  Duration? _routeDuration;

  /// Fare in cents
  int? _fare;
  StreamSubscription<dynamic>? _driverSubscription;
  StreamSubscription<dynamic>? _rideSubscription;
  Driver? _currentDriver;
  Ride? _currentRide;
  final Set<Marker> _markers = {};
  LatLng? _previousDriverLocation;
  Image? _centerPinImage;
  BitmapDescriptor? _pinIcon;
  BitmapDescriptor? _carIcon;

  @override
  void initState() {
    super.initState();
    _signInIfNotSignedIn();
    _checkLocationPermission();
    _loadPinIcon();
    _loadCarIcon();
  }

  @override
  void dispose() {
    _cancelSubscriptions();
    super.dispose();
  }

  Future<void> _signInIfNotSignedIn() async {
    if (_supabase.auth.currentSession == null) {
      try {
        await _supabase.auth.signInAnonymously();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: ${e.toString()}')),
          );
        }
      }
    }
  }

  Future<void> _checkLocationPermission() async {
    bool serviceEnabled;
    LocationPermission permission;

    // Test if location services are enabled.
    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return Future.error('Location services are disabled.');
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return Future.error('Location permissions are denied');
      }
    }

    if (permission == LocationPermission.deniedForever) {
      // Permissions are denied forever, handle appropriately.
      return Future.error(
          'Location permissions are permanently denied, we cannot request permissions.');
    }

    // When we reach here, permissions are granted and we can
    // continue accessing the position of the device.
    _getCurrentLocation();
  }

  Future<void> _getCurrentLocation() async {
    try {
      Position position = await Geolocator.getCurrentPosition();
      setState(() {
        _currentLocation = LatLng(position.latitude, position.longitude);
        _initialPosition = CameraPosition(
          target: _currentLocation!,
          zoom: 14.0,
        );
      });
      _mapController
          .animateCamera(CameraUpdate.newCameraPosition(_initialPosition));
    } catch (e) {
      print("Error getting location: $e");
      // Handle the error appropriately. Maybe set a default location or show an error message to the user.
    }
  }

  Future<void> _loadPinIcon() async {
    const ImageConfiguration imageConfiguration =
        ImageConfiguration(size: Size(48, 48));
    _pinIcon = await BitmapDescriptor.asset(
        imageConfiguration, 'assets/images/pin.png');
  }

  Future<void> _loadCarIcon() async {
    _carIcon = await BitmapDescriptor.asset(
      const ImageConfiguration(size: Size(48, 48)),
      'assets/images/car.png',
    );
  }

  void _goToNextState() {
    setState(() {
      if (_appState.index < AppState.values.length - 1) {
        _appState = AppState.values[_appState.index + 1];
      } else {
        _appState = AppState.choosingLocation;
      }
    });
  }

  void _goToPreviousState() {
    setState(() {
      if (_appState.index > 0) {
        _appState = AppState.values[_appState.index - 1];
      }
    });
  }

  void _onCameraMove(CameraPosition position) {
    if (_appState == AppState.choosingLocation) {
      _selectedDestination = position.target;
    }
  }

  Future<void> _confirmLocation() async {
    if (_selectedDestination != null && _currentLocation != null) {
      try {
        final response = await _supabase.functions.invoke(
          'route',
          body: {
            'origin': {
              'latitude': _currentLocation!.latitude,
              'longitude': _currentLocation!.longitude,
            },
            'destination': {
              'latitude': _selectedDestination!.latitude,
              'longitude': _selectedDestination!.longitude,
            },
          },
        );

        final data = response.data as Map<String, dynamic>;
        final coordinates = data['legs'][0]['polyline']['geoJsonLinestring']
            ['coordinates'] as List<dynamic>;
        final duration = data['duration'] as String;

        final List<LatLng> polylineCoordinates = coordinates.map((coord) {
          return LatLng(coord[1], coord[0]);
        }).toList();

        setState(() {
          _polylines.add(Polyline(
            polylineId: const PolylineId('route'),
            points: polylineCoordinates,
            color: Colors.black,
            width: 5,
          ));
          _routeDuration = parseDuration(duration);
          _fare = ((_routeDuration!.inMinutes * 40)).ceil();

          _markers.add(Marker(
            markerId: const MarkerId('destination'),
            position: _selectedDestination!,
            icon: _pinIcon ??
                BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed),
          ));
        });

        LatLngBounds bounds = LatLngBounds(
          southwest: LatLng(
            polylineCoordinates
                .map((e) => e.latitude)
                .reduce((a, b) => a < b ? a : b),
            polylineCoordinates
                .map((e) => e.longitude)
                .reduce((a, b) => a < b ? a : b),
          ),
          northeast: LatLng(
            polylineCoordinates
                .map((e) => e.latitude)
                .reduce((a, b) => a > b ? a : b),
            polylineCoordinates
                .map((e) => e.longitude)
                .reduce((a, b) => a > b ? a : b),
          ),
        );
        _mapController.animateCamera(CameraUpdate.newLatLngBounds(bounds, 50));
        _goToNextState();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: ${e.toString()}')),
          );
        }
      }
    }
  }

  Future<void> _findDriver() async {
    try {
      final response = await _supabase.rpc('find_driver', params: {
        'origin':
            'POINT(${_currentLocation!.longitude} ${_currentLocation!.latitude})',
        'destination':
            'POINT(${_selectedDestination!.longitude} ${_selectedDestination!.latitude})',
        'fare': _fare,
      }) as List<dynamic>;

      if (response.isNotEmpty) {
        String driverId = response.first['driver_id'];
        String rideId = response.first['ride_id'];

        _driverSubscription = _supabase
            .from('drivers')
            .stream(primaryKey: ['id'])
            .eq('id', driverId)
            .listen((List<Map<String, dynamic>> data) {
              if (data.isNotEmpty) {
                setState(() {
                  _currentDriver = Driver.fromJson(data[0]);
                });
                _updateDriverMarker(_currentDriver!);
                _adjustMapView(
                    target: _appState == AppState.waitingForPickup
                        ? _currentLocation!
                        : _selectedDestination!);
              }
            });

        _rideSubscription = _supabase
            .from('rides')
            .stream(primaryKey: ['id'])
            .eq('id', rideId)
            .listen((List<Map<String, dynamic>> data) {
              if (data.isNotEmpty) {
                setState(() {
                  _currentRide = Ride.fromJson(data[0]);
                  if (_currentRide!.status == RideStatus.riding &&
                      _appState != AppState.riding) {
                    _appState = AppState.riding;
                  } else if (_currentRide!.status == RideStatus.completed &&
                      _appState != AppState.postRide) {
                    _appState = AppState.postRide;
                    _cancelSubscriptions();
                    _showCompletionModal();
                  }
                });
              }
            });

        _goToNextState();
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
                content: Text('No driver found. Please try again later.')),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
      }
    }
  }

  void _updateDriverMarker(Driver driver) {
    setState(() {
      _markers.removeWhere((marker) => marker.markerId.value == 'driver');

      double rotation = 0;
      if (_previousDriverLocation != null) {
        rotation =
            _calculateRotation(_previousDriverLocation!, driver.location);
      }

      _markers.add(Marker(
        markerId: const MarkerId('driver'),
        position: driver.location,
        icon: _carIcon!,
        anchor: const Offset(0.5, 0.5),
        rotation: rotation,
      ));

      _previousDriverLocation = driver.location;
    });
  }

  void _adjustMapView({required LatLng target}) {
    if (_currentDriver != null && _selectedDestination != null) {
      LatLngBounds bounds = LatLngBounds(
        southwest: LatLng(
          min(_currentDriver!.location.latitude, target.latitude),
          min(_currentDriver!.location.longitude, target.longitude),
        ),
        northeast: LatLng(
          max(_currentDriver!.location.latitude, target.latitude),
          max(_currentDriver!.location.longitude, target.longitude),
        ),
      );
      _mapController.animateCamera(CameraUpdate.newLatLngBounds(bounds, 100));
    }
  }

  double _calculateRotation(LatLng start, LatLng end) {
    double latDiff = end.latitude - start.latitude;
    double lngDiff = end.longitude - start.longitude;
    double angle = atan2(lngDiff, latDiff);
    return angle * 180 / pi;
  }

  void _cancelSubscriptions() {
    _driverSubscription?.cancel();
    _rideSubscription?.cancel();
  }

  void _showCompletionModal() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Ride Completed'),
          content: const Text(
              'Thank you for using our service! We hope you had a great ride.'),
          actions: <Widget>[
            TextButton(
              child: const Text('Close'),
              onPressed: () {
                Navigator.of(context).pop();
                _resetAppState();
              },
            ),
          ],
        );
      },
    );
  }

  void _resetAppState() {
    setState(() {
      _appState = AppState.choosingLocation;
      _selectedDestination = null;
      _currentDriver = null;
      _currentRide = null;
      _routeDuration = null;
      _fare = null;
      _polylines.clear();
      _markers.clear();
      _previousDriverLocation = null;
    });
    _getCurrentLocation();
  }

  Widget _buildBottomSheet() {
    switch (_appState) {
      case AppState.confirmingFare:
        return Container(
          width: MediaQuery.of(context).size.width,
          padding: const EdgeInsets.all(16)
              .copyWith(bottom: 16 + MediaQuery.of(context).padding.bottom),
          decoration: BoxDecoration(
            color: Colors.white,
            boxShadow: [
              BoxShadow(
                color: Colors.grey.withOpacity(0.5),
                spreadRadius: 5,
                blurRadius: 7,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Confirm Fare',
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 16),
              Text(
                  'Estimated fare: ${NumberFormat.currency(
                    symbol:
                        '\$', // You can change this to your preferred currency symbol
                    decimalDigits: 2,
                  ).format(_fare! / 100)}',
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _findDriver,
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size(double.infinity, 50),
                ),
                child: const Text('Confirm Fare'),
              ),
            ],
          ),
        );
      case AppState.waitingForPickup:
        if (_currentDriver == null) {
          return const SizedBox.shrink();
        }
        return Container(
          width: MediaQuery.of(context).size.width,
          padding: const EdgeInsets.all(16)
              .copyWith(bottom: 16 + MediaQuery.of(context).padding.bottom),
          decoration: BoxDecoration(
            color: Colors.white,
            boxShadow: [
              BoxShadow(
                color: Colors.grey.withOpacity(0.5),
                spreadRadius: 5,
                blurRadius: 7,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Your Driver',
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              Text('Car: ${_currentDriver!.model}',
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              Text('Plate Number: ${_currentDriver!.number}',
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              Text(
                  'Your driver is on the way. Please wait at the pickup location.',
                  style: Theme.of(context).textTheme.bodyMedium),
            ],
          ),
        );
      default:
        return const SizedBox.shrink();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: _appState != AppState.choosingLocation
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: _goToPreviousState,
              )
            : null,
        title: Text(_getAppBarTitle()),
      ),
      body: Stack(
        children: [
          _currentLocation == null
              ? const Center(child: CircularProgressIndicator())
              : GoogleMap(
                  initialCameraPosition: _initialPosition,
                  onMapCreated: (GoogleMapController controller) {
                    _mapController = controller;
                  },
                  myLocationButtonEnabled: true,
                  myLocationEnabled: true,
                  onCameraMove: _onCameraMove,
                  polylines: _polylines,
                  markers: _markers,
                ),
          if (_appState == AppState.choosingLocation)
            Center(
              child: Image.asset(
                'assets/images/center-pin.png',
                width: 100,
                height: 100,
              ),
            ),
        ],
      ),
      floatingActionButton: _appState == AppState.choosingLocation
          ? FloatingActionButton.extended(
              onPressed: _confirmLocation,
              label: const Text('Confirm Destination'),
              icon: const Icon(Icons.check),
            )
          : null,
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
      bottomSheet: _buildBottomSheet(),
    );
  }

  String _getAppBarTitle() {
    switch (_appState) {
      case AppState.choosingLocation:
        return 'Choose Location';
      case AppState.confirmingFare:
        return 'Confirm Fare';
      case AppState.waitingForPickup:
        return 'Waiting for Pickup';
      case AppState.riding:
        return 'On the Way';
      case AppState.postRide:
        return 'Ride Completed';
    }
  }
}
