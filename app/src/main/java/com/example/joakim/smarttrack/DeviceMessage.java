package com.example.joakim.smarttrack;

import android.os.Build;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;

import java.nio.charset.Charset;

public class DeviceMessage {
    private static final Gson gson = new Gson();

    private String mMessageBody;
    private static LatLng mLatLng;

    public static Message newNearbyMessage(String instanceId) {
        DeviceMessage deviceMessage = new DeviceMessage(instanceId);
        return new Message(gson.toJson(deviceMessage).toString().getBytes(Charset.forName("UTF-8")));
    }

    public static DeviceMessage fromNearbyMessage(Message message) {
        String nearbyMessageString = new String(message.getContent());
        return gson.fromJson(
                (new String(nearbyMessageString.getBytes(Charset.forName("UTF-8")))),
                DeviceMessage.class);
    }

    private DeviceMessage(String instanceId) {
        String mInstanceId = instanceId;
        this.mMessageBody = Build.MODEL;
        mLatLng = new LatLng(59.407620, 17.947907);
    }

    protected String getMessageBody() {
        return mMessageBody;
    }

    public LatLng getmLatLng() {
        return mLatLng;
    }

    public void setmLatLng(LatLng mLatLng) {
        DeviceMessage.mLatLng = mLatLng;
    }
}