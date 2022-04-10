package g2g.diploma.gps_realtime.AU;

import static g2g.diploma.gps_realtime.Constants.ERROR_DIALOG_REQUEST;
import static g2g.diploma.gps_realtime.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static g2g.diploma.gps_realtime.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;

import g2g.diploma.gps_realtime.ClusterMarker;
import g2g.diploma.gps_realtime.R;
import g2g.diploma.gps_realtime.UserClient;
import g2g.diploma.gps_realtime.UserUsage.User;
import g2g.diploma.gps_realtime.UserUsage.UserLocation;
import g2g.diploma.gps_realtime.Utils.ClusterManagerRenderer;
import g2g.diploma.gps_realtime.models.PolylineData;
import g2g.diploma.gps_realtime.services.LocationService;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnPolylineClickListener

{

    Button button_location, mbutton_logout;
    TextView usName,usPhone,usEmail,greeting_tx;
    ImageView button_reset;



    private static final String TAG = "MainActivity";


    private boolean mLocationPermissionGranted = false;

    private static final float DEFAULT_ZOOM_CAMERA = 15f;


    private ListenerRegistration mUserListEventListener;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore mDb;
    private UserLocation mUserLocation;

    private ArrayList<UserLocation> aUserLocations = new ArrayList<>();
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();
    private ArrayList<User> mUserList = new ArrayList<>();

    private GoogleMap mMap;


    private ClusterManager mClusterManager;

    private ClusterManagerRenderer mClusterManagerRenderer;


    private FirebaseUser user;
    private DatabaseReference reference;
    private String userId;

    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    private GeoApiContext mGeoApiContext = null;

    private ArrayList<PolylineData>  mPolylineData = new ArrayList<>();
    private Marker mSelectedMarker = null;

    private ArrayList<Marker> mTripMarkers = new ArrayList<>();







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mDb = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        init();
        uProfile();
        getALlUsers();


    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
            startActivity(new Intent(getApplicationContext(),  MainActivity.class));
            finish();
    }

    private void init() {

        DocumentReference isFullAccessGranted = mDb.collection("User").
                document(FirebaseAuth.getInstance().getUid());




        button_location = findViewById(R.id.gLocation);

        isFullAccessGranted.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (!documentSnapshot.getString("isFullAccess").equals("Not Granted")){

                    button_location.setVisibility(View.VISIBLE);
                    button_location.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            setContentView(R.layout.activity_google_map);
                            initMap();

                            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(getApplicationContext(),
                                    Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED)
                            {
                                return;
                            }
                            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                                @Override
                                public void onComplete(@NonNull Task<Location> task) {
                                    if (task.isSuccessful()) {
                                        Location location = task.getResult();

                                        cameraView(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM_CAMERA);

                                    }
                                }
                            });
                        }
                    });

                }
            }
        });

        button_location.setVisibility(View.INVISIBLE);

        mbutton_logout = findViewById(R.id.button_logout);



        mbutton_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Logged Out Successfully!", Toast.LENGTH_SHORT).show();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Login.class));
                finish();


            }
        });

    }

    private  void cameraView(LatLng latLng,float zoom){
        Log.d(TAG,"moveCamera: moving camera to: " + latLng.latitude + "," + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));


    }

    private void initMap() {

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            mapFragment.getMapAsync(MainActivity.this);



        if (mGeoApiContext == null){
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_key_Key))
                    .build();
        }

    }

    @SuppressLint({"PotentialBehaviorOverride"})
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {


            Toast.makeText(this, "Map Is Ready", Toast.LENGTH_SHORT).show();
            mMap = googleMap;


            button_reset = findViewById(R.id.btn_reset_map);

            button_reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addMapMarkers();
                }
            });


            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("User", mUserList);
            bundle.putParcelableArrayList("User Location",  aUserLocations);



            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.setOnPolylineClickListener(this);


            addMapMarkers();


            startUserLocationRunnable();


    }

    private void startUserLocationRunnable(){
        Log.d(TAG,"startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocation();
                mHandler.postDelayed(mRunnable,LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void retrieveUserLocation() {
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users.");
        try {
            for (final ClusterMarker clusterMarker: mClusterMarkers){
                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection("User Location")
                        .document(clusterMarker.getUser().getUser_id());

                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){

                            final UserLocation updatedUserLocation = task.getResult().toObject(UserLocation.class);

                            //update the location

                            for (int i = 0; i < mClusterMarkers.size();i++){
                                try {
                                    if (mClusterMarkers.get(i).getUser().getUser_id().equals(updatedUserLocation.getUser().getUser_id())){
                                        LatLng updatedLatLng = new LatLng(
                                                updatedUserLocation.getGeo_point().getLatitude(),
                                                updatedUserLocation.getGeo_point().getLongitude()
                                        );

                                        mClusterMarkers.get(i).setPosition(updatedLatLng);
                                        mClusterManagerRenderer.setUpdateMarker(mClusterMarkers.get(i));

                                    }
                                }catch (NullPointerException e ){
                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }catch (IllegalStateException e){
            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage() );
        }
    }

    private void uProfile(){
        usName = findViewById(R.id.userFname);
        usPhone = findViewById(R.id.uPhone);
        usEmail = findViewById(R.id.uEmail);

        greeting_tx = findViewById(R.id.greetingTx);


        user = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("User");
        userId = user.getUid();


        reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);

                if (userProfile != null){
                    String fullName = userProfile.fName;
                    String email = userProfile.email;
                    String phone = userProfile.phone;

                    greeting_tx.setText("Welcome, " + fullName + "!");
                    usName.setText("Name: " + fullName);
                    usPhone.setText("Phone: " + phone);
                    usEmail.setText("Email: " + email);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,"Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        });


    }



    private void getALlUsers(){
        CollectionReference userRef = mDb.collection("User");

        mUserListEventListener = userRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Log.d(TAG,"onEvent: listener  failed!");
                    return;
                }

                if (value != null){
                    mUserList.clear();
                    mUserList = new ArrayList<>();

                    for (DocumentSnapshot doc : value){
                        User user = doc.toObject(User.class);
                        getUserLocation(user);
                        mUserList.add(user);
                    }
                    Log.d(TAG,"onEvent: got'em!" + mUserList.size());

                }
            }
        });
    }

    private void getUserLocation(User user) {
        DocumentReference locationRef = mDb.collection("User Location")
                .document(user.user_id);

        locationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().toObject(UserLocation.class) != null) {
                        aUserLocations.add(task.getResult().toObject(UserLocation.class));
                    }
                }

            }
        });

    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 50;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    private void removeTripMarkers(){
        for (Marker marker: mTripMarkers){
            marker.remove();
        }
    }

    private void resetSelectedMarker(){
        if (mSelectedMarker != null){
            mSelectedMarker.setVisible(true);
            mSelectedMarker = null;
            removeTripMarkers();
        }
    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                if (mPolylineData.size() > 0){
                    for (PolylineData polylineData: mPolylineData){
                        polylineData.getPolyline().remove();
                    }
                    mPolylineData.clear();
                    mPolylineData = new ArrayList<>();
                }

                double duration = 99999999;

                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();


                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(MainActivity.this, R.color.darkGrey));
                    polyline.setClickable(true);
                    mPolylineData.add(new PolylineData(polyline,route.legs[0]));



                    double tempDuration = route.legs[0].duration.inSeconds;
                    if (tempDuration < duration){
                        duration = tempDuration;
                        onPolylineClick(polyline);
                        zoomRoute(polyline.getPoints());
                    }

                    mSelectedMarker.setVisible(false);

                }
            }
        });
    }

    private void calculateDirections(Marker marker){
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);//show all the possible routs not only the fastest one
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude()
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );

            }
        });
    }






    private void startLocationService(){
        if (!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(MainActivity.this, LocationService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
               startForegroundService(serviceIntent);
            }else {
                startService(serviceIntent);
            }



        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("g2g.diploma.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }

    private void resetMap(){
        if(mMap != null) {
            mMap.clear();

            if(mClusterManager != null){
                mClusterManager.clearItems();
            }

            if (mClusterMarkers.size() > 0) {
                mClusterMarkers.clear();
                mClusterMarkers = new ArrayList<>();
            }

            if(mPolylineData.size() > 0){
                mPolylineData.clear();
                mPolylineData = new ArrayList<>();
            }
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    private void addMapMarkers(){

        if (mMap != null ){

            resetMap();

            if (mClusterManager == null){
                mClusterManager = new ClusterManager<ClusterMarker>(MainActivity.this.getApplicationContext(),mMap);
            }
            if (mClusterManagerRenderer == null){
                mClusterManagerRenderer = new ClusterManagerRenderer(MainActivity.this,mMap,mClusterManager);

                mClusterManager.setRenderer(mClusterManagerRenderer);
            }


            mMap.setOnInfoWindowClickListener(this);



            for (UserLocation userLocation: aUserLocations){
                Log.d(TAG,"addMapMarkers: location: " + userLocation.getGeo_point().toString());
                try {
                    String snippet = "";
                    if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                        snippet = "This Is You!";

                    }
                    else {
                        snippet = "Determine route to " + userLocation.getUser().getfName() + "?";


                    }
                    int avatar = R.drawable.ic_findpeoplee;


                    ClusterMarker newClusterMarker = new ClusterMarker(new LatLng(userLocation.getGeo_point().getLatitude(),
                            userLocation.getGeo_point().getLongitude()),
                            userLocation.getUser().getfName(),
                            snippet,
                            avatar,
                            userLocation.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);

                }catch (NullPointerException e){
                    Log.d(TAG,"addMapMarkers: NUllPointException: " + e.getMessage());
                }
            }
            mClusterManager.cluster();


        }
    }

 

    private void saveUserLocation() {
        if (mUserLocation != null) {
            DocumentReference locationRef = mDb.collection("User Location").
                    document(FirebaseAuth.getInstance().getUid());

            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "saveUserLocation: \ninserted user location into database." +
                                "\n latitude: " + mUserLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + mUserLocation.getGeo_point().getLongitude());


                    }
                }
            });
        }
    }

    private void getLastKnowLocation() {
        Log.d(TAG, "getLastKnowLocation: called");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {

                    try{
                        Location location = task.getResult();
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "onComplete last location: latitude: " + geoPoint.getLatitude());
                        Log.d(TAG, "onComplete: last location: longitude: " + geoPoint.getLongitude());

                        mUserLocation.setGeo_point(geoPoint);
                        mUserLocation.setTimestamp(null);
                        saveUserLocation();

                        startLocationService();
                        Toast.makeText(MainActivity.this, "Map loaded!!", Toast.LENGTH_SHORT).show();
                    }catch (NullPointerException e){
                        Log.e(TAG, "onClick: NullPointerException: Map loading..." + e.getMessage() );
                        Toast.makeText(MainActivity.this, "Map loading....", Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, "Wait......", Toast.LENGTH_SHORT).show();



                    }

                }
            }
        });
    }


   @Override
    protected void onResume() {
        super.onResume();
       if (checkMapServices()){
            if (mLocationPermissionGranted){
                getUserDetails();
                startUserLocationRunnable();
            }
            else {
                getLocationPermission();
            }
       }

    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

    }
    private void stopLocationUpdates(){
        Log.d(TAG,"STOPP");
        mHandler.removeCallbacks(mRunnable);
    }





    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;

    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS location to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();

    }

    
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if(mLocationPermissionGranted){

                    getUserDetails();
                }
                else{
                    getLocationPermission();
                }
            }
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. */

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;

            getUserDetails();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }

    private void getUserDetails() {
        if (mUserLocation == null) {
            mUserLocation = new UserLocation();

            DocumentReference userRef = mDb.collection("User").
                    document(FirebaseAuth.getInstance().getUid());


            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {

                        Log.d(TAG, "onComplete: successfully got the user details.");

                        User user = task.getResult().toObject(User.class);
                        mUserLocation.setUser(user);
                        ((UserClient)(getApplicationContext())).setUser(user);

                        getLastKnowLocation();


                    }
                }
            });
        } else {
            getLastKnowLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++){
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        int index = 0;
        for(PolylineData polylineData: mPolylineData){
            index++;
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(MainActivity.this, R.color.blue1));
                polylineData.getPolyline().setZIndex(1);

                LatLng endLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );

                Marker marker = mMap.addMarker(new MarkerOptions()
                .position(endLocation)
                .title("Trip: #" + index)
                                .snippet("Duration: "+ polylineData.getLeg().duration)
                );


                marker.showInfoWindow();

                mTripMarkers.add(marker);
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(MainActivity.this, R.color.darkGrey));
                polylineData.getPolyline().setZIndex(0);
            }
        }
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {

        if(marker.getTitle().contains("Trip: #")){
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Open Google Maps?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            String latitude = String.valueOf(marker.getPosition().latitude);
                            String longitude = String.valueOf(marker.getPosition().longitude);
                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");

                            try{
                                if (mapIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                                    startActivity(mapIntent);
                                }
                            }catch (NullPointerException e){
                                Log.e(TAG, "onClick: NullPointerException: Couldn't open map." + e.getMessage() );
                                Toast.makeText(MainActivity.this, "Couldn't open map", Toast.LENGTH_SHORT).show();
                            }

                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            if(marker.getSnippet().equals("This is you")){
                marker.hideInfoWindow();
            }
            else{
                Log.d(TAG,"Go on");
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);/////
                builder.setMessage(marker.getSnippet())
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                                resetSelectedMarker();
                                mSelectedMarker = marker;
                                calculateDirections(marker);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                dialog.cancel();
                            }
                        });
                final AlertDialog alert = builder.create();
                alert.show();
            }

        }
    }
}