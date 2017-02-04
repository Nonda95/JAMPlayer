package pl.osmalek.bartek.jamplayer.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import pl.osmalek.bartek.jamplayer.App;

public class SplashActivity extends PermissionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_PERMISSION);
    }

    @Override
    protected void permissionDenied(int requestCode) {
        finish();
    }

    @Override
    protected void permissionGranted(int requestCode) {
        App.get().permissionGranted();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
