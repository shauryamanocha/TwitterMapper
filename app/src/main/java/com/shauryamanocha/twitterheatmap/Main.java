package com.shauryamanocha.twitterheatmap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.twitter.sdk.android.core.Twitter;

import java.util.ArrayList;
import java.util.Random;

import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import static java.lang.Math.random;

public class Main extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public static final String
    CONSUMER_KEY = "aF29Unxpm97NcVPdemkr36aah",
    CONSUMER_SECRET = "1PNFTbdGGP7ng8Gv7GyyIXZ63AVRl2I3z2sqNZLQLNaqNYRW2K";
    String[] permissions = new String[]{
      Manifest.permission.INTERNET
    };
    long maxId = 0;

    ArrayList<LatLng> heatmapPoints = new ArrayList<>();
    HeatmapTileProvider heatmapTileProvider;
    TileOverlay overlay;
    int tweets;

    public static final ConfigurationBuilder cb = new ConfigurationBuilder()
            .setDebugEnabled(true)
            .setOAuthAccessToken("1009892782570434561-kuiMahdcD8Sp4rWUb85nSQpHIki5aO")
            .setOAuthAccessTokenSecret("CiOnAaKppxAO2VvKzOYbFDbbYZVGRKowRNJjodCKMSbxz")
            .setOAuthConsumerKey(CONSUMER_KEY)
            .setOAuthConsumerSecret(CONSUMER_SECRET);
    public static final TwitterFactory twitterFactory = new TwitterFactory(cb.build());
    public static final twitter4j.Twitter twitter = twitterFactory.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        if (android.os.Build.VERSION.SDK_INT > 23) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }






        final EditText keywordText = findViewById(R.id.keyword);
        Button searchButton = findViewById(R.id.search);
        final EditText locations = findViewById(R.id.locations);
        final EditText searches = findViewById(R.id.searches);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Log.w("findTweets",findTweets(keywordText.getText().toString(),Integer.parseInt(locations.getText().toString()),Integer.parseInt(searches.getText().toString())));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.w("error","");
                }
                heatmapPoints.clear();
                if(overlay!=null) {
                    overlay.clearTileCache();
                }
            }
        });





        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,permissions,1);
        }else {

        }


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;



//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

    }

    String findTweets(String keyword,int locations, int searches) throws InterruptedException {
        for(int lat = 0;lat<Math.floor(Math.sqrt(locations));lat++) {
            for (int lng = 0; lng < Math.floor(Math.sqrt(locations)); lng++) {
                LatLng currentPos = new LatLng(-90+(180 / Math.floor(Math.sqrt(locations))) * lat, -180+(360 / Math.floor(Math.sqrt(locations))) * lng);
                Log.w("pos", currentPos.toString() + " : " + lat);
                try {
                    Query search = new Query(keyword);
                    search.setCount(100);
                    //search.sinceId(maxId);
                    search.geoCode(new GeoLocation(currentPos.latitude, currentPos.longitude), 5000/locations, "mi");
                    QueryResult result = twitter.search(search);
                    for (Status status : result.getTweets()) {
                        //Circle currentZone = mMap.addCircle(new CircleOptions()
                              //  .center(currentPos)
                                //.radius(100));

                        tweets++;
                        if (status.getId() > maxId) {
                            maxId = status.getId();
                        }
                        Log.w("TAG", status.getText());
                        if (status.getGeoLocation() != null) {
                            Log.w("heatmap", status.getGeoLocation() + "");
                            heatmapPoints.add(new LatLng(status.getGeoLocation().getLatitude(), status.getGeoLocation().getLongitude()));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(status.getGeoLocation().getLatitude(), status.getGeoLocation().getLongitude())));
                            //mMap.addMarker(new MarkerOptions().position(new LatLng(status.getGeoLocation().getLatitude(), status.getGeoLocation().getLongitude())).title(status.getText()));
                        } else {
                            heatmapPoints.add(new LatLng(currentPos.latitude,currentPos.latitude));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPos));
                            //mMap.addMarker(new MarkerOptions().position(new LatLng(currentPos.latitude,currentPos.latitude)).title(status.getText()));

                            if (heatmapPoints.size() != 0) {
                                heatmapTileProvider = new HeatmapTileProvider.Builder()
                                        .data(heatmapPoints)
                                        .build();
                                heatmapTileProvider.setRadius(200);
                                overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(heatmapPoints.get(heatmapPoints.size() - 1)));
                                overlay.clearTileCache();
                            }
                            //currentZone.remove();
                        }
                    }

                    //return "Searching for " + keyword+ " With "+ searches+" searches per "+ locations+" locations ";
                } catch (TwitterException e) {
                    e.printStackTrace();
                    Thread.sleep(e.getRateLimitStatus().getSecondsUntilReset()*1000);

                    lng--;
                }

            }





        }

        Log.w("heatmap", heatmapPoints.size() + " at " + heatmapPoints.toString());
        Log.w("test", tweets + "");

        return "end";
    }
}
