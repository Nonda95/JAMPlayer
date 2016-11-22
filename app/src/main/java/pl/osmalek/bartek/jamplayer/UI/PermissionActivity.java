package pl.osmalek.bartek.jamplayer.UI;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.widget.Toast;

import pl.osmalek.bartek.jamplayer.R;

/**
 * Created by osmalek on 21.10.2016.
 */

public abstract class PermissionActivity extends AppCompatActivity implements DialogInterface.OnClickListener {
    static final int READ_EXTERNAL_STORAGE_PERMISSION = 1;

    boolean before;

    public void requestPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            permissionGranted(requestCode);
            return;
        }

        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                if (before = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialog_Light));
                    dialogBuilder.setMessage(R.string.permission_message)
                            .setTitle(R.string.permission_title)
                            .setPositiveButton("OK", this)
                            .setCancelable(false);
                    AlertDialog dialog = dialogBuilder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
                }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted(requestCode);
                } else {
                    if (!(before || ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]))) {
                        Toast.makeText(this, "Nadaj uprawnienia", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    permissionDenied(requestCode);
                }
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
    }

    protected abstract void permissionDenied(int requestCode);

    protected abstract void permissionGranted(int requestCode);
}
