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
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.launch

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
    supabaseUrl = "https://bxqrmzvwscpmnimeeesf.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ4cXJtenZ3c2NwbW5pbWVlZXNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE2OTI3NTU4NDAsImV4cCI6MjAwODMzMTg0MH0.GZBcMyenIRwCyyFdapZQGSX07iIjjitUU0Zsus1an50"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}

@Composable
fun HomePage(name: String, modifier: Modifier = Modifier) {
    val markerState = rememberMarkerState(position = LatLng(1.35, 103.87))

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerState.position, 16f)
    }
    val context = LocalContext.current

    val composableScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                markerState.position = LatLng(1.3501, 103.87)

//                val client = HttpClient()
                composableScope.launch {

//                    val response: HttpResponse =
//                        client.request("https://routes.googleapis.com/directions/v2:computeRoutes") {
//                            method = HttpMethod.Post
//                            headers {
//                                append(
//                                    "X-Goog-FieldMask",
//                                    "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline"
//                                )
//                                append("X-Goog-Api-Key", "")
//                            }
//                            contentType(ContentType.Application.Json)
//                            setBody(mapOf("a" to "f"))
//                        }
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
                points = listOf(

                    LatLng(1.34819, 103.87223),
                    LatLng(1.36, 103.87),
                )
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

