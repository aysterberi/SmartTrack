package com.example.joakim.smarttrack;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import java.util.ArrayList;

public class MainFragment extends Fragment
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainFragment";
    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(Constants.TTL_IN_SECONDS)
            .build();

    private ProgressBar mSubscriptionProgressBar;
    private ImageButton mSubscriptionImageButton;
    private ProgressBar mPublicationProgressBar;
    private ImageButton mPublicationImageButton;

    private ArrayAdapter<String> mNearbyDevicesArrayAdapter;
    private final ArrayList<String> mNearbyDevicesArrayList = new ArrayList<>();

    private GoogleApiClient mGoogleApiClient;
    private Message mDeviceInfoMessage;
    private MessageListener mMessageListener;

    private boolean mResolvingNearbyPermissionError = false;

    private MapView mMapView;
    private GoogleMap mMap;
    private Bundle mBundle;

    public MainFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundle = savedInstanceState;
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);
        mSubscriptionProgressBar = (ProgressBar) view.findViewById(R.id.subscription_progress_bar);
        mSubscriptionImageButton = (ImageButton) view.findViewById(R.id.subscription_image_button);
        mSubscriptionImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subscriptionTask = getPubSubTask(Constants.KEY_SUBSCRIPTION_TASK);
                if(TextUtils.equals(subscriptionTask, Constants.TASK_NONE) ||
                        TextUtils.equals(subscriptionTask, Constants.TASK_UNSUBSCRIBE)) {
                    updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_SUBSCRIBE);
                } else {
                    updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_UNSUBSCRIBE);
                }
            }
        });

        mPublicationProgressBar = (ProgressBar) view.findViewById(R.id.publication_progress_bar);
        mPublicationImageButton = (ImageButton) view.findViewById(R.id.publication_image_button);
        mPublicationImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String publicationTask = getPubSubTask(Constants.KEY_PUBLICATION_TASK);
                if (TextUtils.equals(publicationTask, Constants.TASK_NONE) ||
                        TextUtils.equals(publicationTask, Constants.TASK_UNPUBLISH)) {
                    updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_PUBLISH);
                } else {
                    updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_UNPUBLISH);
                }
            }
        });

        final ListView nearbyDevicesListView = (ListView) view.findViewById(
                R.id.nearby_devices_list_view);
        mNearbyDevicesArrayAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1, mNearbyDevicesArrayList);
        nearbyDevicesListView.setAdapter(mNearbyDevicesArrayAdapter);

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(final Message message) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNearbyDevicesArrayAdapter.add(
                                DeviceMessage.fromNearbyMessage(message).getMessageBody());
                        LatLng position = DeviceMessage.fromNearbyMessage(message).getmLatLng();
                        mMap.addMarker(new MarkerOptions().position(position).title(
                                DeviceMessage.fromNearbyMessage(message).getMessageBody()));
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Message: " + DeviceMessage.fromNearbyMessage(message).getMessageBody()
                                        + " " + DeviceMessage.fromNearbyMessage(message).getmLatLng(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }


            @Override
            public void onLost(final Message message) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNearbyDevicesArrayAdapter.remove(
                                DeviceMessage.fromNearbyMessage(message).getMessageBody());
                    }
                });
            }
        };

        MapsInitializer.initialize(getActivity());
        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(mBundle);
        setUpMapIfNeeded(view);
        mMap.setMyLocationEnabled(true);

        updateUI();
        return view;
    }

    private void setUpMapIfNeeded(View view) {
        if (mMap == null) {
            mMap = ((MapView) view.findViewById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
    }


    protected void finishedResolvingNearbyPermissionError() {
        mResolvingNearbyPermissionError = false;
    }

    private void clearDeviceList() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNearbyDevicesArrayAdapter.clear();
            }
        });
    }

    public void updatePosition() {
        try {
            if (mMap != null) {
                DeviceMessage.fromNearbyMessage(mDeviceInfoMessage).setmLatLng(new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude()));
                Toast.makeText(getActivity().getApplicationContext(),
                        "My position: " + DeviceMessage.fromNearbyMessage(mDeviceInfoMessage).getmLatLng(),
                        Toast.LENGTH_LONG).show();
            }
        } catch(NullPointerException npe) {
            npe.printStackTrace();
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().getPreferences(Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);
        mDeviceInfoMessage = DeviceMessage.newNearbyMessage(
                InstanceID.getInstance(getActivity().getApplicationContext()).getId());
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if(mGoogleApiClient.isConnected() && !getActivity().isChangingConfigurations()) {
            unsubscribe();
            unpublish();
            updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_NONE);
            updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_NONE);

            mGoogleApiClient.disconnect();
            getActivity().getPreferences(Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        executePendingTasks();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended: "
                + connectionSuspendedCauseToString(cause));
    }

    private static String connectionSuspendedCauseToString(int cause) {
        switch (cause) {
            case CAUSE_NETWORK_LOST:
                return "CAUSE_NETWORK_LOST";
            case CAUSE_SERVICE_DISCONNECTED:
                return "CAUSE_SERVICE_DISCONNECTED";
            default:
                return "CAUSE_UNKNOWN: " + cause;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "connection to GoogleApiClient failed");
    }

    private String getPubSubTask(String taskKey) {
        return getActivity()
                .getPreferences(Context.MODE_PRIVATE)
                .getString(taskKey, Constants.TASK_NONE);
    }

    void executePendingTasks() {
        executePendingSubscriptionTask();
        executePendingPublicationTask();
    }

    void executePendingSubscriptionTask() {
        String pendingSubscriptionTask = getPubSubTask(Constants.KEY_SUBSCRIPTION_TASK);
        if(TextUtils.equals(pendingSubscriptionTask, Constants.TASK_SUBSCRIBE)) {
            subscribe();
        } else if(TextUtils.equals(pendingSubscriptionTask, Constants.TASK_UNSUBSCRIBE)) {
            unsubscribe();
        }
    }

    void executePendingPublicationTask() {
        String pendingPublicationTask = getPubSubTask(Constants.KEY_PUBLICATION_TASK);
        if(TextUtils.equals(pendingPublicationTask, Constants.TASK_PUBLISH)) {
            publish();
        } else if(TextUtils.equals(pendingPublicationTask, Constants.TASK_UNPUBLISH)) {
            unpublish();
        }
    }

    private void subscribe() {
        Log.i(TAG, "trying to subscribe");

        if(!mGoogleApiClient.isConnected()) {
            if(!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            clearDeviceList();
            final SubscribeOptions options = new SubscribeOptions.Builder()
                    .setStrategy(PUB_SUB_STRATEGY)
                    .setCallback(new SubscribeCallback() {
                        @Override
                        public void onExpired() {
                            super.onExpired();
                            Log.i(TAG, "no longer subscribing");
                            updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_NONE);
                        }
                    }).build();
            Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if(status.isSuccess()) {
                                Log.i(TAG, "subscribed successfully");

                            } else {
                                Log.i(TAG, "could not subscribe");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void unsubscribe() {
        Log.i(TAG, "trying to unsubscribe");

        if(!mGoogleApiClient.isConnected()) {
            if(!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if(status.isSuccess()) {
                                Log.i(TAG, "unsubscribed successfully");
                                updateSharedPreference(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_NONE);
                            } else {
                                Log.i(TAG, "could not unsubscribe");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void publish() {
        //updatePosition();
        Log.i(TAG, "trying to publish");

        if(!mGoogleApiClient.isConnected()) {
            if(!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            PublishOptions options = new PublishOptions.Builder()
                    .setStrategy(PUB_SUB_STRATEGY)
                    .setCallback(new PublishCallback() {

                        @Override
                        public void onExpired() {
                            super.onExpired();
                            Log.i(TAG, "no longer publishing");
                            updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_NONE);
                        }
                    }).build();

            Nearby.Messages.publish(mGoogleApiClient, mDeviceInfoMessage, options)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if(status.isSuccess()) {
                                //updatePosition();
                                Log.i(TAG, "published successfully");
                            } else {
                                Log.i(TAG, "could not publish");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void unpublish() {
        Log.i(TAG, "trying to unpublish");

        if(!mGoogleApiClient.isConnected()) {
            if(!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            Nearby.Messages.unpublish(mGoogleApiClient, mDeviceInfoMessage)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if(status.isSuccess()) {
                                Log.i(TAG, "unpublished successfully");
                                updateSharedPreference(Constants.KEY_PUBLICATION_TASK, Constants.TASK_NONE);
                            } else {
                                Log.i(TAG, "could not unpublish");
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
    }

    private void handleUnsuccessfulNearbyResult(Status status) {
        Log.i(TAG, "processing error, status = " + status);
        if(status.getStatusCode() == NearbyMessagesStatusCodes.APP_NOT_OPTED_IN) {
            if(!mResolvingNearbyPermissionError) {
                try {
                    mResolvingNearbyPermissionError = true;
                    status.startResolutionForResult(getActivity(), Constants.REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException sie) {
                    sie.printStackTrace();
                }
            }
        } else {
            if(status.getStatusCode() == ConnectionResult.NETWORK_ERROR) {
                Toast.makeText(getActivity().getApplicationContext(),
                        "No connectivity, cannot proceed. Fix in 'Settings' and try again.",
                        Toast.LENGTH_LONG).show();
                resetToDefaultState();
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Unsuccessful: " +
                status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (TextUtils.equals(key, Constants.KEY_SUBSCRIPTION_TASK)) {
                    executePendingSubscriptionTask();
                } else if (TextUtils.equals(key, Constants.KEY_PUBLICATION_TASK)) {
                    executePendingPublicationTask();
                }
                updateUI();
            }
        });
    }
    void resetToDefaultState() {
        getActivity().getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString(Constants.KEY_SUBSCRIPTION_TASK, Constants.TASK_NONE)
                .putString(Constants.KEY_PUBLICATION_TASK, Constants.TASK_NONE)
                .apply();
    }

    private void updateUI() {
        String subscriptionTask = getPubSubTask(Constants.KEY_SUBSCRIPTION_TASK);
        String publicationTask = getPubSubTask(Constants.KEY_PUBLICATION_TASK);

        mSubscriptionProgressBar.setVisibility(
                TextUtils.equals(subscriptionTask, Constants.TASK_SUBSCRIBE)
                        ? View.VISIBLE : View.INVISIBLE);
        mPublicationProgressBar.setVisibility(
                TextUtils.equals(publicationTask, Constants.TASK_PUBLISH)
                        ? View.VISIBLE : View.INVISIBLE);

        mSubscriptionImageButton.setImageResource(
                TextUtils.equals(subscriptionTask, Constants.TASK_SUBSCRIBE)
                        ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
        mPublicationImageButton.setImageResource(
                TextUtils.equals(publicationTask, Constants.TASK_PUBLISH)
                        ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
    }

    private void updateSharedPreference(String key, String value) {
        getActivity().getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }

}
