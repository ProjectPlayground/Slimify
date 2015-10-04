package com.thepurryness.slimify;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.VideoView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;

public class MainActivity extends Activity {
    static final String TAG = "MainActivity";

    //GOOGLE FIT API AUTHENTHACATION 
    private static final int REQUEST_OAUTH = 1;

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;


    //VIDEO AS THE BACKGROUND AT LOGIN PAGE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VideoView videoView = (VideoView) findViewById(R.id.lp_videoView);
        String UrlPath="android.resource://"+getPackageName()+"/"+R.raw.loginpage;
        videoView.setVideoURI(Uri.parse(UrlPath));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        videoView.start();


        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        buildFitnessClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // GOOGLE FIT API
    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or having
     *  multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
            .addApi(Fitness.API)
            .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
            .addConnectionCallbacks(
                    new GoogleApiClient.ConnectionCallbacks() {

                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.i(TAG, "Connected!!!");
                            // Now you can make calls to the Fitness APIs.
                            // Put application specific code here.
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            // If your connection to the sensor gets lost at some point,
                            // you'll be able to determine the reason and react to it here.
                            if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                            } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                            }
                        }
                    }
            )
            .addOnConnectionFailedListener(
                    new GoogleApiClient.OnConnectionFailedListener() {
                        // Called whenever the API client fails to connect.
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Connection failed. Cause: " + result.toString());
                            if (!result.hasResolution()) {
                                // Show the localized error dialog
                                GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                        MainActivity.this, 0).show();
                                return;
                            }
                            // The failure has a resolution. Resolve it.
                            // Called typically when the app is not yet authorized, and an
                            // authorization dialog is displayed to the user.
                            if (!authInProgress) {
                                try {
                                    Log.i(TAG, "Attempting to resolve failed connection");
                                    authInProgress = true;
                                    result.startResolutionForResult(MainActivity.this,
                                            REQUEST_OAUTH);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e(TAG,
                                            "Exception while starting resolution activity", e);
                                }
                            }
                        }
                    }
            )
            .build();
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Connecting...");
        mClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mClient.isConnected()) {
            mClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
}
