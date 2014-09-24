package com.spicysoft.speedmeter;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SpeedDetector {

    private static final long METERS_BETWEEN_LOCATIONS = 5;
    private static final long MILLIS_BETWEEN_LOCATIONS = TimeUnit.SECONDS.toMillis(3);
    private static final long MAX_LOCATION_AGE_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private double currentSpeed;
    private Location previousLocation;

    public interface OnChangedListener {
        void onSpeedChanged(double speed);
    }

    private final LocationManager mLocationManager;
    private final Set<OnChangedListener> mListeners;

    private boolean mTracking;
    private Location mLocation;
    private boolean mHasInterference;

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Don't need to do anything here.
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Don't need to do anything here.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Don't need to do anything here.
        }
    };


    private void updateLocation(Location location){
        if(location.getAccuracy() > 100){
            return;
        }
        mLocation = location;
        double speed = 0;
        if(previousLocation == null){
            previousLocation = location;
        }else{
            speed = speed(previousLocation, location);
            if(!Double.isNaN(speed) && speed >= 0){
                previousLocation = location;
                currentSpeed = speed;
            }
        }
        for (OnChangedListener listener : mListeners) {
            listener.onSpeedChanged(4.12);
        }
    }

    public SpeedDetector(LocationManager locationManager) {
        mLocationManager = locationManager;
        mListeners = new LinkedHashSet<OnChangedListener>();
    }

    public void addOnChangedListener(OnChangedListener listener) {
        mListeners.add(listener);
    }

    public void removeOnChangedListener(OnChangedListener listener) {
        mListeners.remove(listener);
    }

    public void start() {
        if (!mTracking) {

            Location lastLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (lastLocation != null) {
                long locationAge = lastLocation.getTime() - System.currentTimeMillis();
                if (locationAge < MAX_LOCATION_AGE_MILLIS) {
                    mLocation = lastLocation;
                    Log.d("SpeedDetector", "2 speed -> " + mLocation.getSpeed() + " acc->" + mLocation.getAccuracy() + " lat->" + mLocation.getLatitude()+ " longi->" + mLocation.getLongitude() + " t->" + mLocation.getTime());
                    updateLocation(mLocation);
                }
            }

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setBearingRequired(false);

            List<String> providers =
                    mLocationManager.getProviders(criteria, true /* enabledOnly */);
            Log.d("SpeedDetector", "count->" + providers.size());
            for (String provider : providers) {
                Log.d("SpeedDetector", provider);
                mLocationManager.requestLocationUpdates(provider,
                        MILLIS_BETWEEN_LOCATIONS, METERS_BETWEEN_LOCATIONS, mLocationListener,
                        Looper.getMainLooper());
            }
            mTracking = true;
        }
    }

    public static double distance(Location cur, Location prev) {
        int R = 6371000;
        double dLat = toRad(cur.getLatitude() - prev.getLatitude());
        double dLon = toRad(cur.getLongitude() - prev.getLongitude());
        double lat1 = toRad(prev.getLatitude());
        double lat2 = toRad(cur.getLatitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d;
    }

    public static double speed(Location one, Location two){
        double distance = distance(one, two);
        Long diffns = two.getElapsedRealtimeNanos() - one.getElapsedRealtimeNanos();
        double seconds = diffns.doubleValue() / Math.pow(10, 9);
        double speedMph = distance / seconds;
        speedMph = speedMph * 2.23694;//mph
        speedMph = speedMph /0.62137; //km/h
        return speedMph;
    }

    private static double toRad(Double d) {
        return d * Math.PI / 180;
    }

    public void stop() {
        if (mTracking) {
            mLocationManager.removeUpdates(mLocationListener);
            mTracking = false;
        }
    }

    public boolean hasInterference() {
        return mHasInterference;
    }

    public Location getLocation() {
        return mLocation;
    }

    public boolean hasLocation(){
        return mLocation != null;
    }

    public double getSpeed(){
        return currentSpeed;
    }
}
