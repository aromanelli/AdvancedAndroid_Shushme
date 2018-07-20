package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

// TODO (1) Create a Geofencing class with a Context and GoogleApiClient constructor that
// initializes a private member ArrayList of Geofences called mGeofenceList

// https://developer.android.com/training/location/geofencing
public class Geofencing implements ResultCallback {

    public static final String TAG = Geofencing.class.getSimpleName();
    private static final float GEOFENCE_RADIUS = 50; // 50 meters
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours

    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    // private GeofencingClient mGeofenceClient;

    public Geofencing(GoogleApiClient client, Context context) {
        this.mGoogleApiClient = client;
        this.mContext = context;
        this.mGeofencePendingIntent = null;
        this.mGeofenceList = new ArrayList<>();

        // GeofencingApi interface was deprecated.  Use the GoogleApi-based API GeofencingClient instead.
        // https://developer.android.com/training/location/geofencing
        // this.mGeofenceClient = LocationServices.getGeofencingClient(mContext);
    }

    // TODO (2) Inside Geofencing, implement a public method called updateGeofencesList that
    // given a PlaceBuffer will create a Geofence object for each Place using Geofence.Builder
    // and add that Geofence to mGeofenceList

    public void updateGeofencesList(PlaceBuffer places) {
        // Create a Geofence for each Place ...
        mGeofenceList = new ArrayList<>();
        if (places == null || places.getCount() == 0) return;
        for (Place place : places) {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(place.getId())
                    .setExpirationDuration(GEOFENCE_TIMEOUT) // Never Expire
                    .setCircularRegion(
                            place.getLatLng().latitude,
                            place.getLatLng().longitude,
                            GEOFENCE_RADIUS
                    )
                    .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            mGeofenceList.add(geofence);
        }
        // For a production app should setup a job scheduler and reregister geofences every day.
    }

    // TODO (3) Inside Geofencing, implement a private helper method called getGeofencingRequest that
    // uses GeofencingRequest.Builder to return a GeofencingRequest object from the Geofence list
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // If already inside a geofence, do a entry trigger event ...
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        // INITIAL_TRIGGER_DWELL, device has to be inside the geofence for some duration of time
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    // TODO (5) Inside Geofencing, implement a private helper method called getGeofencePendingIntent that
    // returns a PendingIntent for the GeofenceBroadcastReceiver class

    private PendingIntent getGeofencePendingIntent() {
        // Reuse PendingIntent if we already have it ...
        if (mGeofencePendingIntent == null) {
            mGeofencePendingIntent = PendingIntent.getBroadcast(
                    mContext,
                    0,
                    new Intent(mContext, GeofenceBroadcastReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }
        return mGeofencePendingIntent;
    }

    // TODO (6) Inside Geofencing, implement a public method called registerAllGeofences that
    // registers the GeofencingRequest by calling LocationServices.GeofencingApi.addGeofences
    // using the helper functions getGeofencingRequest() and getGeofencePendingIntent()

    public void registerAllGeofences() {

        // Check API client is connected and list has Geofences in it ...
        if (!isValidClient() ||
                ((mGeofenceList == null) || (mGeofenceList.size() == 0)) ) {
            return;
        }

        try {
            // GeofencingApi interface was deprecated.  Use the GoogleApi-based API GeofencingClient instead.
            // https://developer.android.com/training/location/geofencing
            // LocationServices.getGeofencingClient(this), 'this' activity/context
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException se) {
            Log.e(TAG, "registerAllGeofences: ", se);
        }

    }

    // TODO (7) Inside Geofencing, implement a public method called unRegisterAllGeofences that
    // unregisters all geofences by calling LocationServices.GeofencingApi.removeGeofences
    // using the helper function getGeofencePendingIntent()

    public void unRegisterAllGeofences() {
        if (!isValidClient()) {
            return;
        }
        try {
            // GeofencingApi interface was deprecated.  Use the GoogleApi-based API GeofencingClient instead.
            // https://developer.android.com/training/location/geofencing
            // LocationServices.getGeofencingClient(this), 'this' activity/context
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException se) {
            Log.e(TAG, "unRegisterAllGeofences: ", se);
        }
    }

    private boolean isValidClient() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG,
                String.format("onResult: Error adding/removing geofence : %s", result.getStatus().toString()));
    }

}
