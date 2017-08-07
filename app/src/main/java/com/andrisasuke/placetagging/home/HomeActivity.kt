package com.andrisasuke.placetagging.home

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.TextView
import com.andrisasuke.placetagging.BaseActivity
import com.andrisasuke.placetagging.adapter.TagPlacesAdapter
import com.andrisasuke.placetagging.splash.SplashActivity
import com.andrisasuke.placetagging.tag.TagLocationActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.andrisasuke.placetagging.R
import kotlinx.android.synthetic.main.home.*

class HomeActivity: BaseActivity(), OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    companion object {
        val TAG = "HomeActivity"
        val REQUEST_CHECK_SETTINGS = 14
        val APP_PERMISSION_ACCESS_LOCATION = 10
    }

    private val toolbar: Toolbar by lazy { findViewById(R.id.toolbar) as Toolbar }
    private lateinit var signOutTxt: TextView

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mLocationRequest: LocationRequest
    private var mLocation: Location? = null
    private var mCurrLocationMarker: Marker? = null
    private lateinit var adapter: TagPlacesAdapter
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleApiClient;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        renderView()

        val googleSignInOpt = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

        googleSignInClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOpt)
                .build()

        val query: Query = FirebaseDatabase.getInstance().reference.child("events_tag_places")
        adapter = TagPlacesAdapter(this, query, mutableListOf(), mutableListOf())

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL;
        list_places.layoutManager = layoutManager
        list_places.adapter = adapter
    }

    private fun renderView() {
        progressBar.visibility = View.GONE
        tag_location_main.setOnClickListener { tagLocation() }
        lat_lon.text = getString(R.string.lat_lon_f, "0.0", "0,0")
        signOutTxt = toolbar.findViewById(R.id.sign_out_tv)

        signOutTxt.setOnClickListener{
            Log.d(TAG, "On click Sign Out")
            signOut()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        Log.d(TAG, "google map is ready")
        googleMap = map!!
        var defaultLoc = LatLng(-6.175392, 106.827153)
        if (localPreferences.getLastLocation() != null) {
            val lastLoc = localPreferences.getLastLocation()
            defaultLoc = LatLng(java.lang.Double.valueOf(lastLoc!!.first)!!, java.lang.Double.valueOf(lastLoc.second)!!)
        }

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        googleMap.setMinZoomPreference(5f)
        if (hasLocationPermission()) {
            setMyLocationEnabled()
            googleApiClientConnect()
        } else dialogLocationEnabled()
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLoc))
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17f))

        googleMap.setOnMyLocationButtonClickListener {
            requestLocationUpdate()
            true
        }
    }

    override fun onLocationChanged(location: Location?) {
        Log.d(TAG, "location is changed ${location.toString()}")
        if (location == null) return
        mLocation = location

        lat_lon.text = getString(R.string.lat_lon_f, location.latitude.toString(),
                location.longitude.toString())

        mCurrLocationMarker?.remove()

        localPreferences.storeLastLocation(location.latitude.toString(),
                location.longitude.toString())

        val latLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.flat(true)
        markerOptions.position(latLng)
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_me))
        mCurrLocationMarker = googleMap.addMarker(markerOptions)

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17f))

        //stop location updates
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
        } catch (e: Exception) {
            Log.e(TAG, "failed to remove location update, ${e.message}")
        }

    }

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "google client is connected")
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 1000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        if(hasLocationPermission())
            requestLocationUpdate();
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.e(TAG, "google client is suspended: $p0")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e(TAG, "google client is failed, ${p0.errorMessage}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.destroy()
        if (mGoogleApiClient.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this)
            mGoogleApiClient.disconnect()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            APP_PERMISSION_ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Access location permission is granted")
                onLocationPermissionGranted()
            } else
                dialogLocationEnabled()
        }
    }

    private fun dialogLocationEnabled() {
        if (!hasLocationPermission()) {
            simpleDialog(null, getString(R.string.location_access_permission), false, false,
                    DialogInterface.OnClickListener { dialog, which ->
                        askLocationPermission()
                        dialog.dismiss()
                    })
        }
    }

    fun hasLocationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }

    private fun askLocationPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                APP_PERMISSION_ACCESS_LOCATION)
    }

    fun onLocationPermissionGranted() {
        setMyLocationEnabled()
        googleApiClientConnect()
    }

    fun setMyLocationEnabled() {
        try {
            googleMap.isMyLocationEnabled = true
            Log.d(TAG, "MyLocation enabled")
        } catch (se: SecurityException) {
            Log.e(TAG, "Failed set myLocation, ${se.message}")
        }
    }

    fun requestLocationUpdate() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
            Log.d(TAG, "Location update requested")
        } catch (se: SecurityException) {
            Log.e(TAG, "Request location update failed, ${se.message}")
        }

    }

    private fun googleApiClientConnect() {
        mGoogleApiClient.connect()

        val request = LocationRequest()
        request.interval = 1000
        request.fastestInterval = 1000
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(request)
        val result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build())

        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> Log.d(TAG, "user location is already success")
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    Log.w(TAG, "user location RESOLUTION_REQUIRED")
                    try {
                        status.startResolutionForResult(this@HomeActivity, REQUEST_CHECK_SETTINGS)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "failed to startResolutionForResult, ${e.message}")
                    }

                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.w(TAG, "user location SETTINGS_CHANGE_UNAVAILABLE")
            }
        }
    }

    private fun tagLocation() {

        if(mLocation != null) {
            val intent = Intent(this, TagLocationActivity::class.java)
            intent.putExtra(TagLocationActivity.LATITUDE_PARAM, mLocation!!.latitude.toString())
            intent.putExtra(TagLocationActivity.LONGITUDE_PARAM, mLocation!!.longitude.toString())
            startActivity(intent)
        }
    }

    private fun signOut() {
        simpleDialog("Sign Out", "Are you sure to sign out?", true, true, DialogInterface.OnClickListener {
            dialog, which ->
            adapter.destroy()
            firebaseAuth.signOut()
            // Google sign out
            Log.d(TAG, "signing out from GoogleSignInApi.")
            Auth.GoogleSignInApi.signOut(googleSignInClient).setResultCallback {
                    status -> Log.d(TAG, "GoogleSignInApi signout result, ${status.statusCode}")
                }
            localPreferences.destroy()
            backToSplash()
        })
    }

    private fun backToSplash(){
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }
}