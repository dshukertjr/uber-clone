package com.supabase.uberclone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UberCloneTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomePage("Uber Clone")
                }

                // A surface container using the 'background' color from the theme

            }
        }
    }
}

val supabase = createSupabaseClient(
//    supabaseUrl = "http://127.0.0.1:54321",
    supabaseUrl = "http://10.0.2.2:54321",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Functions)
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
    val legs: Array<Leg>
)


@Composable
fun HomePage(name: String, modifier: Modifier = Modifier) {
    val markerState = rememberMarkerState(position = LatLng(1.35, 103.87))

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerState.position, 16f)
    }
    val context = LocalContext.current

    val composableScope = rememberCoroutineScope()

    var polylinePoints by remember { mutableStateOf(emptyList<LatLng>()) }


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
//                markerState.position = LatLng(1.3501, 103.87)

                composableScope.launch {
                    val position = cameraPositionState.position.target
                    println(position)
                    val res = supabase.functions.invoke(
                        function = "route",
                        body = buildJsonObject {
                            putJsonObject("origin") {
                                put("latitude", markerState.position.latitude)
                                put("longitude", markerState.position.longitude)
                            }
                            putJsonObject("destination") {
                                put("latitude", position.latitude)
                                put("longitude", position.longitude)
                            }
                        }
                    )

                    val data = res.body<RouteResponse>()

                    println(data)
                    polylinePoints = data.legs[0].polyline.geoJsonLinestring.coordinates.map {
                        LatLng(it[1], it[0])
                    }
//                    polylinePoints = listOf(
//                        LatLng(1.35, 103.87),
//                        LatLng(1.354, 103.81)
//                    )
                    println("polyline below")
                    println(polylinePoints.toString())
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) {
        it
        GoogleMap(
            modifier = Modifier
                .fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = PackageManager.PERMISSION_GRANTED == checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style),
            )
        ) {
            Polyline(
                width = 20f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                points = polylinePoints
//                listOf(
//
//                    LatLng(1.34819, 103.87223),
//                    LatLng(1.36, 103.87),
//                )
            )
            MapMarker(
                state = markerState,
                title = "Car",
                context = LocalContext.current,
                iconResourceId = R.drawable.car,
            )
        }
    }
}

@Composable
fun MapMarker(
    context: Context,

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
        rotation = 20f,
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

