@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class
)

package com.supabase.uberclone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.supabase.uberclone.ui.theme.UberCloneTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.postgresSingleDataFlow
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Duration

val supabase = createSupabaseClient(
//    supabaseUrl = "http://127.0.0.1:54321",
//    supabaseUrl = "http://10.0.2.2:54321",
//    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"
    supabaseUrl = "https://rmjwhnhfotnpbnjfnxka.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJtandobmhmb3RucGJuamZueGthIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTkzNzMzODYsImV4cCI6MjAzNDk0OTM4Nn0.l5C85uPdndHNWDQPXr8OPPBOjqCgnsn2bvhWtiTy328"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Functions)
}

/** Holds the different phase of the app */
enum class AppPhase {
    SELECT_DESTINATION,
    CONFIRM_FAIR,
    WAITING_PICKUP,
    IN_CAR,
    AFTER_RIDE,
}


@Serializable
data class GeoJsonLinestring(
    val coordinates: Array<Array<Double>>
)

@Serializable
data class Polyline(
    val geoJsonLinestring: GeoJsonLinestring
)

@Serializable
data class Leg(
    val polyline: Polyline
)

@Serializable
data class RouteResponse(
    val legs: Array<Leg>,
    val duration: String
)

@Serializable
data class Driver(
    val id: String,
    val model: String,
    val number: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
enum class RideStatus {
    @SerialName("picking_up")
    PickingUp,

    @SerialName("riding")
    Riding,

    @SerialName("completed")
    Completed,
}

@Serializable
data class Ride(
    val id: String,
    val status: RideStatus
)

@Serializable
data class FindDriverResponse(
    @SerialName("driver_id")
    val driverId: String,
    @SerialName("ride_id")
    val rideId: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UberCloneTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    HomePage()
                    TestPage()
                }

                // A surface container using the 'background' color from the theme

            }
        }
    }
}

@Composable
fun TestPage() {
    val composableScope = rememberCoroutineScope()
    val composableScope2 = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            composableScope.launch {
                @Serializable
                data class Message(val content: String, val sender: String)

                val channel = supabase.channel("channelId") {
                    // optional config
                }

                val broadcastFlow = channel.broadcastFlow<Message>(event = "message")

                // Collect the flow
                broadcastFlow.onEach { //it: Message
                    println(it)
                }.launchIn(composableScope)

                channel.subscribe(blockUntilSubscribed = true)

                channel.broadcast(event = "message", Message("I joined!", "John"))

                val channel = supabase.channel("channelId")
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public")

                channel.subscribe()

                changeFlow.collect {
                    when (it) {
                        is PostgresAction.Delete -> println("Deleted: ${it.oldRecord}")
                        is PostgresAction.Insert -> println("Inserted: ${it.record}")
                        is PostgresAction.Select -> println("Selected: ${it.record}")
                        is PostgresAction.Update -> println("Updated: ${it.oldRecord} with ${it.record}")
                    }
                }
//                    .launchIn(composableScope)

            }
        }) {
            Text("press")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage() {
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            supabase.auth.signInAnonymously()
        }
    }

    val carMarkerState = rememberMarkerState(position = LatLng(40.7531074, -73.9940147))
    val destinationMarkerState = rememberMarkerState(position = LatLng(40.7531074, -73.9940147))


    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(carMarkerState.position, 14f)
    }

    val context = LocalContext.current

    /** Whether to show the user's location or not */
    var isMyLocationEnabled by remember {
        mutableStateOf(
            PackageManager.PERMISSION_GRANTED == checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
    }

    var appPhase by remember { mutableStateOf(AppPhase.SELECT_DESTINATION) }

    var origin by remember { mutableStateOf(LatLng(0.0, 0.0)) }

    var tripDuration by remember { mutableStateOf(Duration.ZERO) }

    var isLoading by remember { mutableStateOf(false) }

    var driver by remember { mutableStateOf<Driver?>(null) }

    val composableScope = rememberCoroutineScope()

    var polylinePoints by remember { mutableStateOf(emptyList<LatLng>()) }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val permissionsGranted =
                permissions.values.reduce { acc, isPermissionGranted ->
                    acc && isPermissionGranted
                }

            if (permissionsGranted) {
                println("my location enabled")
                isMyLocationEnabled = true
                getLocation(context = context, onLocationResult = {
                    println(it.latitude)
                    println(it.longitude)
                    cameraPositionState.move(
                        update = CameraUpdateFactory.newLatLng(
                            LatLng(
                                it.latitude,
                                it.longitude
                            )
                        )
                    )
                })
            } else {
                Toast.makeText(context, "Please enable location access", Toast.LENGTH_SHORT).show()
            }
        })
    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (appPhase == AppPhase.SELECT_DESTINATION) {

                Button(onClick = {

                    appPhase = AppPhase.CONFIRM_FAIR
                    isLoading = true

                    val cameraPosition = cameraPositionState.position.target

                    destinationMarkerState.position = cameraPosition

                    getLocation(context = context, onLocationResult = {
                        origin = LatLng(it.latitude, it.longitude)
                        composableScope.launch {
                            val res = supabase.functions.invoke(
                                function = "route",
                                body = buildJsonObject {
                                    putJsonObject("origin") {
                                        put("latitude", it.latitude)
                                        put("longitude", it.longitude)
                                    }
                                    putJsonObject("destination") {
                                        put("latitude", cameraPosition.latitude)
                                        put("longitude", cameraPosition.longitude)
                                    }
                                }
                            )

                            val data = res.body<RouteResponse>()

                            polylinePoints =
                                data.legs[0].polyline.geoJsonLinestring.coordinates.map {
                                    LatLng(it[1], it[0])
                                }
                            isLoading = false

                            // Move the camera so that the entire path is present
                            val builder = LatLngBounds.Builder()
                            polylinePoints.forEach { point ->
                                builder.include(point)
                            }
                            val bounds = builder.build()

                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                            )

                            // Calculate fair
                            tripDuration = Duration.parse(data.duration)
                        }
                    })
                }) {
                    Text("Take me here")
                }
            }
        }
    ) { it ->
        Box(modifier = Modifier.padding(it)) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    val locationPermissionsAlreadyGranted = checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (locationPermissionsAlreadyGranted) {

                        // move camera to the location
                        getLocation(context = context, onLocationResult = {
                            cameraPositionState.move(
                                update = CameraUpdateFactory.newLatLng(
                                    LatLng(it.latitude, it.longitude)
                                )
                            )
                        })
                    } else {
                        locationPermissionLauncher.launch(locationPermissions)
                    }
                },
                properties = MapProperties(
                    isMyLocationEnabled = isMyLocationEnabled,
                    mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                        context,
                        R.raw.map_style
                    ),
                )
            ) {
                Polyline(
                    width = 16f,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    points = polylinePoints
                )
                if (appPhase == AppPhase.WAITING_PICKUP || appPhase == AppPhase.IN_CAR) {
                    MapMarker(
                        state = carMarkerState,
                        title = "Car",
                        context = LocalContext.current,
                        iconResourceId = R.drawable.car,
                    )
                }
                if (appPhase == AppPhase.CONFIRM_FAIR || appPhase == AppPhase.WAITING_PICKUP || appPhase == AppPhase.IN_CAR)
                    MapMarker(
                        state = destinationMarkerState,
                        title = "Pin",
                        context = LocalContext.current,
                        iconResourceId = R.drawable.pin,
                    )

            }
            if (appPhase == AppPhase.SELECT_DESTINATION) {
                Image(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(120.dp)
                        .height(120.dp),
                    painter = painterResource(R.drawable.pin_center),
                    contentDescription = "Map Pin"
                )
            }
        }
        if (appPhase == AppPhase.CONFIRM_FAIR) {
            ModalBottomSheet(
                onDismissRequest = {
                    appPhase = AppPhase.SELECT_DESTINATION
                },
            ) {
                // Sheet content
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .align(if (isLoading) Alignment.CenterHorizontally else Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 24.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(64.dp),
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm the trip")
                            Text(
                                "${tripDuration.inWholeMinutes.toInt()}min",
                                Modifier
                                    .background(
                                        Color.Black
                                    )
                                    .padding(2.dp), color = Color.White
                            )
                        }
                        Text(
                            "$${getFareString(tripDuration)}", fontSize = 24.sp
                        )
                        Button(onClick = {
                            appPhase = AppPhase.WAITING_PICKUP
                            // Find nearby taxi
                            composableScope.launch {
                                val destinationLocation = destinationMarkerState.position
                                val findRideResponse =
                                    supabase.postgrest.rpc("find_driver", buildJsonObject {
                                        put(
                                            "origin",
                                            "POINT(${origin.longitude} ${origin.latitude})"
                                        )
                                        put(
                                            "destination",
                                            "POINT(${destinationLocation.longitude} ${destinationLocation.latitude})"
                                        )
                                        put("fare", calculateFare(tripDuration))
                                    }).decodeSingleOrNull<FindDriverResponse>()

                                println("ride ID \uD83C\uDF89\uD83C\uDF89\uD83C\uDF89")
                                println(findRideResponse)
                                if (findRideResponse == null) {
                                    Toast.makeText(
                                        context,
                                        "There are no available driver right now. Please try again later.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                // setup realtime listener
                                val channel = supabase.channel("ride")

//                                val rideFlow: Flow<Ride> = channel.postgresSingleDataFlow(
//                                    schema = "public",
//                                    table = "rides",
//                                    primaryKey = Ride::id
//                                ) {
//                                    //This is the same filter as used in postgrest, so you could use complex queries, but only one entry is fetched
//                                    eq("id", findRideResponse.rideId)
//                                }
//
//                                rideFlow.collect {
//                                    println("rideFlow")
//                                    println(it)
//                                    it.status
//                                }

                                val driverFlow: Flow<Driver> = channel.postgresSingleDataFlow(
                                    schema = "public",
                                    table = "drivers",
                                    primaryKey = Driver::id
                                ) {
                                    eq("id", findRideResponse.driverId)
                                }

                                driverFlow.collect {
                                    println(it)
                                    driver = it
                                }

//                                val changeFlow = channel.postgresChangeFlow<PostgresAction>(
//                                    schema = "public",
//                                ) {
//                                    table = "drivers"
//                                    filter("id", FilterOperator.EQ, findRideResponse.driverId)
//                                }
//
////Collect the flow
//                                changeFlow.collect {
//                                    println(it)
//                                    when (it) {
//                                        is PostgresAction.Delete -> println("Deleted: ${it.oldRecord}")
//                                        is PostgresAction.Insert -> println("Inserted: ${it.record}")
//                                        is PostgresAction.Select -> println("Selected: ${it.record}")
//                                        is PostgresAction.Update -> println("Updated: ${it.oldRecord} with ${it.record}")
//                                    }
//                                }

                                channel.subscribe()
                            }
                        }) {
                            Text("Confirm Pickup")
                        }
                    }
                }
            }
        } else if (appPhase == AppPhase.WAITING_PICKUP) {
            ModalBottomSheet(onDismissRequest = {
                appPhase = AppPhase.CONFIRM_FAIR
            }) {
                if (driver == null) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Box(Modifier.padding(bottom = 24.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(64.dp),
                            )
                        }
                    }
                } else {
                    Column(Modifier.padding(24.dp)) {
                        Text("A ride is on your way", fontSize = 20.sp)
                        val labelFontSize = 12.sp
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text("Car Model", fontSize = labelFontSize)
                                Text(driver!!.model)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Vehicle Number", fontSize = labelFontSize)
                                Text(driver!!.number)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Returns the trip fair in cents. */
private fun calculateFare(duration: Duration): Int {
    return (duration.inWholeMinutes * 40).toInt()
}

private fun getFareString(duration: Duration): String {
    val fare = calculateFare(duration)
    if (fare < 100) {
        return "0.${fare}"
    }
    val fareString = fare.toString()
    return "${
        fareString.substring(
            0,
            fareString.length - 2
        )
    }.${fareString.substring(fareString.length - 2)}"
}


@SuppressLint("MissingPermission")
private fun getLocation(context: Context, onLocationResult: (Location) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    println("here 😂")
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {

                onLocationResult(location)
            } else {
                println("Null location")

            }
        }
    } catch (error: Exception) {
        println("❌❌❌❌❌❌❌")
        println(error)

        return
    }

}


@Composable
fun MapMarker(
    context: Context,
    rotation: Float = 0f,
    state: MarkerState,
    title: String,
    @DrawableRes iconResourceId: Int
) {
    val icon = bitmapDescriptorFromVector(
        context, iconResourceId
    )
    Marker(
        state = state,
        title = title,
        icon = icon,
        rotation = rotation,
    )
}

fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int
): BitmapDescriptor? {

    val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    drawable.setBounds(0, 0, 250, 250)
    val bm = Bitmap.createBitmap(
        250,
        250,
        Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}

