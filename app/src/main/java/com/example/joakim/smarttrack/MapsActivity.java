package com.example.joakim.smarttrack;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MapsActivity extends AppCompatActivity {

    private static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";
    private MainFragment mMainFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        FragmentManager fm = getFragmentManager();
        mMainFragment = (MainFragment) fm.findFragmentByTag(MAIN_FRAGMENT_TAG);

        if(mMainFragment == null) {
            mMainFragment = new MainFragment();
            fm.beginTransaction().add(R.id.container, mMainFragment, MAIN_FRAGMENT_TAG).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMainFragment.finishedResolvingNearbyPermissionError();
        if(requestCode == Constants.REQUEST_RESOLVE_ERROR) {
            if(resultCode == Activity.RESULT_OK) {
                mMainFragment.executePendingTasks();
            } else if(resultCode == Activity.RESULT_CANCELED) {
                mMainFragment.resetToDefaultState();
            } else {
                Toast.makeText(this, "Failed to resolve error with code " + resultCode,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}