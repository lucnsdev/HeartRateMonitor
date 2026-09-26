package lucns.heartratemonitor.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import lucns.heartratemonitor.R;
import lucns.heartratemonitor.utils.Notify;

public class PermissionActivity extends Activity {

    private String[] PERMISSIONS_RUNTIME = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissions();
    }

    private void requestPermissions() {
        String[] deniedPermissions = getDeniedPermissions();
        if (deniedPermissions.length > 0) {
            requestPermissions(deniedPermissions, 1234);
            return;
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String[] getDeniedPermissions() {
        List<String> permissions = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();
        for (String permission : PERMISSIONS_RUNTIME) {
            if (packageManager.checkPermission(permission, packageName) != PackageManager.PERMISSION_GRANTED)
                permissions.add(permission);
        }
        return permissions.toArray(new String[permissions.size()]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Notify.showToast(permissions[i]);
                finish();
                break;
            }
        }
    }
}