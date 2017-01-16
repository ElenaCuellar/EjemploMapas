package com.example.caxidy.ejemplomapas;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.LocationManager;
import android.location.LocationListener;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap; //el mapa que vamos a utilizar

    //Posicionamiento
    private LocationManager locManager;
    private LocationListener locListener;

    //Posicion en el mapa
    private Location posicionActual = null;

    //zoom desde el 2 (el mas alto) hasta el 21 (nivel de calle)
    private int zoom = 2;

    //Tipo de mapa
    private int tipomapa = 0;

    SupportMapFragment mapFragment;

    //ArrayList que almacena los marcadores de la ruta de tres puntos
    ArrayList<LatLng> arrayRuta = new ArrayList<>();

    final String TAG = "PathGoogleMapActivity";

    //Polyline que forma la ruta especificada
    Polyline polilinea;

    //ArrayList que almacena los marcadores que se recuperan al reanudar la actividad
    ArrayList<Marker> arrayMarkers = new ArrayList<>();

    //Texto que aparece en los marcadores
    String txMarker;
    //Latitud y longitud que guardaremos en preferencias para ir a un punto dado (por defecto la UGR)
    Double latitudPref, longitudPref;

    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtenerPosicion();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Para obtener la ruta
        String url = getMapsApiDirectionsUrl();
        ReadTask downloadTask = new ReadTask();
        downloadTask.execute(url);

        //Recuperar la ruta y la informacion necesaria con SharedPreferences
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        int size = sharedPref.getInt("size",0);
        LatLng posic;
        if(size >=1) {
            posic = new LatLng(Double.parseDouble(sharedPref.getString("lat1","0")),Double.parseDouble(sharedPref.getString("long1","0")));
            arrayRuta.add(posic);
        }
        if(size >=2) {
            posic = new LatLng(Double.parseDouble(sharedPref.getString("lat2","0")),Double.parseDouble(sharedPref.getString("long2","0")));
            arrayRuta.add(posic);
        }
        if(size ==3) {
            posic = new LatLng(Double.parseDouble(sharedPref.getString("lat3","0")),Double.parseDouble(sharedPref.getString("long3","0")));
            arrayRuta.add(posic);
        }

        //Seleccionar el texto de los marcadores y otras preferencias
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        txMarker = sp.getString("textomarcador",getString(R.string.textoDefaultMarker));
        latitudPref = Double.parseDouble(sp.getString("latP",getString(R.string.latPrefs)));
        longitudPref = Double.parseDouble(sp.getString("lgP",getString(R.string.longPrefs)));

    }

    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("size",arrayRuta.size());
        if(arrayRuta.size()>=1) {
            editor.putString("lat1", Double.toString(arrayRuta.get(0).latitude));
            editor.putString("long1", Double.toString(arrayRuta.get(0).longitude));
        }
        if (arrayRuta.size()>=2) {
            editor.putString("lat2", Double.toString(arrayRuta.get(1).latitude));
            editor.putString("long2", Double.toString(arrayRuta.get(1).longitude));
        }
        if(arrayRuta.size()==3) {
            editor.putString("lat3", Double.toString(arrayRuta.get(2).latitude));
            editor.putString("long3", Double.toString(arrayRuta.get(2).longitude));
        }
        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Comprobar permiso para versiones 6.0
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
        //Seleccionar el texto de los marcadores
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        txMarker = sp.getString("textomarcador",getString(R.string.textoDefaultMarker));
        latitudPref = Double.parseDouble(sp.getString("latP",getString(R.string.latPrefs)));
        longitudPref = Double.parseDouble(sp.getString("lgP",getString(R.string.longPrefs)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obtenerPosicion();
                }
                return;
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // CONFIGURACION DE GOOGLE MAPS
        // Tipo de Mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // Aparece el botón para situarnos en el mapa mediante un circulo azul y hacer zoom sobre nuestra posición
        mMap.setMyLocationEnabled(true);
        // Controles de zoom
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Aparece la brujula cuando giramos el mapa
        mMap.getUiSettings().setCompassEnabled(true);
        //Se permite la inclinacion
        mMap.getUiSettings().setTiltGesturesEnabled(true);

        // Listener de los eventos que detectan pulsaciones sobre la pantalla
        mapFragment.getMap().setOnMapClickListener(new OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng posicion)
            {
                mMap.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title(txMarker+"(onMapClick)"));
                if(arrayRuta.size()<3)
                    arrayRuta.add(posicion);
                comprobarRuta();
            }
        });
        mMap.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng posicion) {
                mMap.addMarker(new MarkerOptions()
                        .position(posicion)
                        .title(txMarker+"(onMapLongClick)")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.
                                HUE_BLUE)));
                if(arrayRuta.size()<3)
                    arrayRuta.add(posicion);
                comprobarRuta();
            }
        });

        // Add a marker in Sydney and move the camera
        LatLng granada = new LatLng(37.18,-3.62);
        mMap.addMarker(new MarkerOptions().position(granada).title(getString(R.string.markerGr)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(granada));

        //Llamar a comprobar la ruta para dibujarla y poner sus marcadores en caso de haber girado la pantalla
        comprobarRuta();
        colocarMarcadoresRuta();
    }

    private String getMapsApiDirectionsUrl() {
        //Pasar los puntos de la ruta
        String waypoints = "waypoints=optimize:true|"
                + arrayRuta.get(0).latitude + "," + arrayRuta.get(0).longitude
                + "|" + "|" + arrayRuta.get(1).latitude + ","
                + arrayRuta.get(1).longitude + "|" + arrayRuta.get(2).latitude + ","
                + arrayRuta.get(2).longitude;

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        return url;
    }

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(2);
                polyLineOptions.color(Color.BLUE);
            }

            mMap.addPolyline(polyLineOptions);
        }
    }

    private void comprobarRuta(){ //!!
        /*if(arrayRuta.size()==3){
            //Dibujar la ruta
            polilinea = mMap.addPolyline(new PolylineOptions().addAll(arrayRuta).width(15).color(Color.BLUE).geodesic(true));
        }*/
    }

    private void colocarMarcadoresRuta(){
        arrayMarkers.clear();
        for(int i=0;i<arrayRuta.size();i++) {
            Marker mk = mMap.addMarker(new MarkerOptions()
                    .position(arrayRuta.get(i))
                    .title(txMarker+"(recuperado)")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            arrayMarkers.add(mk);
        }
    }

    private void obtenerPosicion() {
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Nos registramos para recibir actualizaciones de la posición
        locListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                System.out.println(getString(R.string.nuevaPos));
                posicionActual = location;
                Toast.makeText(getApplicationContext(), getString(R.string.nuevaPos) +" "+
                                posicionActual.getLatitude() + " , " + posicionActual.getLongitude()
                        , Toast.LENGTH_LONG).show();
            }

            public void onProviderDisabled(String provider) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

        // Comprobamos si el GPS esta activo
        Boolean isGPSEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            try {
                // Obtenemos nueva posicion por GPS
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, locListener);
                if (posicionActual == null) {
                    // Obtenemos la última posición conocida por GPS
                    posicionActual = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Toast.makeText(getApplicationContext(), getString(R.string.ultimaPosGps) , Toast.LENGTH_LONG).show();
                    System.out.println(getString(R.string.ultimaPosGps));
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.posGps) , Toast.LENGTH_LONG).show();
                }
            }catch(SecurityException e){
                System.out.println(getString(R.string.securityGps));
            }
        }
        else {
            try{
                // Obtenemos nueva posicion por RED
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, locListener);
                if (posicionActual == null) {
                // Obtenemos la última posición conocida por RED
                    posicionActual = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Toast.makeText(getApplicationContext(), getString(R.string.ultimaPosRed),Toast.LENGTH_LONG).show();
                    System.out.println(getString(R.string.ultimaPosRed));
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.posRed),Toast.LENGTH_LONG).show();
                }
            }catch(SecurityException e){
                System.out.println(getString(R.string.securityRed));
            }
        }
        try {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, locListener);
        }catch (SecurityException e){
            System.out.println(getString(R.string.securityEx));
        }
    }

    private void addMarcador(LatLng position, String titulo, String info) {
        // Comprueba si hemos obtenido el mapa correctamente
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(position) // posicion clase LatLon
                    .title(titulo) // titulo
                    .snippet(info) // subtitulo
            );
        }
    }

    public void bt_localizame(View v) {
        // Comprobamos si hemos obtenido el MAPA correctamente
        if (mMap != null) {
            // Comprobamos si hemos obtenido NUESTRA POSICION ACTUAL ( ULTIMA OBTENIDA ) correctamente
            if (posicionActual != null) {
                addMarcador(new LatLng(posicionActual.getLatitude(),
                        posicionActual.getLongitude()),getString(R.string.tituloPos), getString(R.string.snippetPos));
            }
            else {
                Toast.makeText(getApplicationContext(), getString(R.string.posNula), Toast.LENGTH_SHORT);
                System.out.println(getString(R.string.posNula));
            }
        }
    }

    public void bt_zoom(View v) {
        // Comprobamos si hemos obtenido el MAPA correctamente
        if (mMap != null) {
            // Obtenemos la posicion de la camara ( donde estamos enfocando actualmente )
            CameraPosition cp = mMap.getCameraPosition();
            // Obtenemos su posicion en Latitud , Longitud
            LatLng posicion = cp.target;
            // Obtenemos el zoom
            zoom = (int) cp.zoom;
            // Aumentamos el Zoom
            if (zoom < 21) {zoom++;};
            // Nos situamos en una posicion y le asignamos un zoom
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, zoom));
        }
    }

    public void bt_tipomapa(View v) {
        // Comprobamos si hemos obtenido el MAPA correctamente
        if (mMap != null) {
            tipomapa = (tipomapa + 1) % 4;
            switch(tipomapa)
            {
                case 0:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case 1:
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
                case 2:
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case 3:
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    break;
            }
        }
    }

    // Movimiento de camara a una posicion que tenemos guardada en preferencias, realizando el movimiento
    public void bt_animateCamera(View v) {
        // Comprobamos si hemos obtenido el MAPA correctamente
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitudPref,longitudPref), 15));
            addMarcador(new LatLng(latitudPref, longitudPref),getString(R.string.posFav), getString(R.string.posFavSnippet));
        }
    }

    //Borrar la ruta creada
    public void bt_borrarRuta(View v){
        AlertDialog.Builder alertDialogBu = new AlertDialog.Builder(this);
        alertDialogBu.setTitle(getString(R.string.borrarRuta));
        alertDialogBu.setMessage(getString(R.string.msDiag));
        alertDialogBu.setIcon(R.mipmap.ic_launcher);
        alertDialogBu.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        });
        alertDialogBu.setPositiveButton(getString(R.string.aceptar), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if(polilinea != null)
                    polilinea.remove();
                for(int i=0;i<arrayMarkers.size();i++)
                    arrayMarkers.get(i).remove();

                arrayMarkers.clear();
                arrayRuta.clear();
            }
        });
        AlertDialog alertDialog = alertDialogBu.create();
        alertDialog.show();
    }
    //Lanzar la actividad del StreetView
    public void bt_verStreetview(View v){
        Intent intent = new Intent(this, StreetViewActivity.class);
        if(posicionActual != null) {
            intent.putExtra("latitudActual", posicionActual.getLatitude());
            intent.putExtra("longitudActual", posicionActual.getLongitude());
        }
        else{
            //Le pasamos una posicion por defecto:
            intent.putExtra("latitudActual", 37.18);
            intent.putExtra("longitudActual",-3.62);
        }
        startActivity(intent);
    }
    //Editar el texto de los marcadores y otras preferencias
    public void bt_editar(View v){
        //Abrir preferencias...
        Intent i = new Intent(this,ActivityPref.class);
        startActivity(i);
    }
}
