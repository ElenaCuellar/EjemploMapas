package com.example.caxidy.ejemplomapas;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;

public class StreetViewActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String EXTRA_LONG = "current_long";
    private static final String EXTRA_LAT = "current_lat";
    private static final String EXTRA_BEARING = "current_bearing";
    private static final String EXTRA_TILT = "current_tilt";
    private static final String EXTRA_ZOOM = "current_zoom";

    private GoogleApiClient mLocationClient;
    private LatLng mCurrentLocation;
    private float mBearing; //orientacion
    private float mTilt; //inclinacion
    private float mZoom; //zoom

    private StreetViewPanorama mPanorama;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.streetview_activity);
        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(EXTRA_LAT) && savedInstanceState.containsKey(EXTRA_LONG)) {
                mCurrentLocation = new LatLng(savedInstanceState.getDouble(EXTRA_LAT), savedInstanceState.getDouble(EXTRA_LONG));
                if(savedInstanceState.containsKey(EXTRA_TILT) && savedInstanceState.containsKey(EXTRA_BEARING)) {
                    mTilt = savedInstanceState.getFloat(EXTRA_TILT);
                    mBearing = savedInstanceState.getFloat(EXTRA_BEARING);
                    mZoom = savedInstanceState.getFloat(EXTRA_ZOOM);
                }
            }
        }
        else{
            Bundle extras = getIntent().getExtras();
            if(extras != null)
                mCurrentLocation = new LatLng(extras.getDouble("latitudActual"), extras.getDouble("longitudActual"));
        }
    }
    @Override
    public void onBackPressed(){finish();}

    //Configuracion del fragmento del StreetView
    private void initStreetView( ) {
        StreetViewPanoramaFragment fragment = ((StreetViewPanoramaFragment) getFragmentManager().findFragmentById( R.id.street_view_panorama_fragment));
        if(mPanorama == null) {
            if(fragment != null) {
                mPanorama = fragment.getStreetViewPanorama();
                if(mPanorama != null && mCurrentLocation != null) {
                    System.out.println(getString(R.string.svposdisp));
                    StreetViewPanoramaCamera.Builder builder = new StreetViewPanoramaCamera.Builder(mPanorama.getPanoramaCamera());
                    if(mBearing != builder.bearing)
                        builder.bearing = mBearing;
                    if(mTilt != builder.tilt)
                        builder.tilt = mTilt;
                    if(mZoom != builder.zoom)
                        builder.zoom = mZoom;
                    mPanorama.animateTo(builder.build(), 0);
                    mPanorama.setPosition(mCurrentLocation, 300);
                    mPanorama.setStreetNamesEnabled(true);
                    mPanorama.setZoomGesturesEnabled(true);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mPanorama != null && mPanorama.getLocation() != null && mPanorama.getLocation().position != null) {
            outState.putDouble(EXTRA_LAT, mPanorama.getLocation().position.latitude);
            outState.putDouble(EXTRA_LONG, mPanorama.getLocation().position.longitude);
        }
        if(mPanorama != null && mPanorama.getPanoramaCamera() != null) {
            outState.putFloat(EXTRA_TILT, mPanorama.getPanoramaCamera().tilt);
            outState.putFloat(EXTRA_BEARING, mPanorama.getPanoramaCamera().bearing);
            outState.putFloat(EXTRA_ZOOM, mPanorama.getPanoramaCamera().zoom);
        }
    }

    private void actualizarStreetViewPosition() {
        if(mLocationClient == null)
            mLocationClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(final Bundle bundle) {
                    if(mCurrentLocation == null) {
                        mCurrentLocation = new LatLng(LocationServices.FusedLocationApi.getLastLocation(mLocationClient).getLatitude(),
                                LocationServices.FusedLocationApi.getLastLocation(mLocationClient).getLatitude());
                    }
                    initStreetView();
                }

                @Override
                public void onConnectionSuspended(int cause) {
                    if (BuildConfig.DEBUG)
                        Toast.makeText(getApplicationContext(),getString(R.string.svsusp)+" ("+cause+")",Toast.LENGTH_SHORT).show();
                }
            }).build();
        if(!mLocationClient.isConnected() && !mLocationClient.isConnecting())
            mLocationClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int cause) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(),getString(R.string.svfail),Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarStreetViewPosition();
    }
}
