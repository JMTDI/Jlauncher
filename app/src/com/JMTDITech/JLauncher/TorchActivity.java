package com.JMTDITech.JLauncher;

import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/** Invisible activity that toggles the flashlight and closes. */
public class TorchActivity extends Activity {
    private CameraManager cm;
    private CameraManager.TorchCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean finished;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cm = (CameraManager) getSystemService(CAMERA_SERVICE);
        String camId = null;
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (flash != null && flash && facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    camId = id;
                    break;
                }
            }
        } catch (Exception e) {
            camId = null;
        }
        if (camId == null) {
            Toast.makeText(this, "No flashlight on this device.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final String target = camId;
        callback = new CameraManager.TorchCallback() {
            @Override public void onTorchModeChanged(String id, boolean enabled) {
                if (finished || !target.equals(id)) return;
                finished = true;
                try {
                    cm.setTorchMode(target, !enabled);
                } catch (Exception e) {
                    Toast.makeText(TorchActivity.this, "Flashlight unavailable.", Toast.LENGTH_SHORT).show();
                }
                done();
            }
            @Override public void onTorchModeUnavailable(String id) {
                if (finished || !target.equals(id)) return;
                finished = true;
                Toast.makeText(TorchActivity.this, "Flashlight unavailable.", Toast.LENGTH_SHORT).show();
                done();
            }
        };
        cm.registerTorchCallback(callback, handler);
        // Safety net so this transparent activity can never linger.
        handler.postDelayed(new Runnable() {
            @Override public void run() { done(); }
        }, 2000);
    }

    private void done() {
        if (callback != null) {
            try { cm.unregisterTorchCallback(callback); } catch (Exception ignored) { }
            callback = null;
        }
        if (!isFinishing()) finish();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
