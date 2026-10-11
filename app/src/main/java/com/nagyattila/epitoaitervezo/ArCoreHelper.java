package com.nagyattila.epitoaitervezo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;

public final class ArCoreHelper {
    private static final String TAG = "ArCoreHelper";
    private static Boolean depthSupportedCache;

    private ArCoreHelper() {
    }

    public static ArCoreApk.Availability check(Context context) {
        try {
            ArCoreApk.Availability checkAvailability = ArCoreApk.getInstance().checkAvailability(context);
            Log.i(TAG, "availability=" + checkAvailability);
            return checkAvailability;
        } catch (Throwable th) {
            Log.w(TAG, "checkAvailability failed", th);
            return ArCoreApk.Availability.UNKNOWN_ERROR;
        }
    }

    public static boolean isDeviceUnsupported(Context context) {
        return check(context) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE;
    }

    public static boolean isFatalUnsupportedFailure(Throwable th) {
        if (th == null) {
            return false;
        }
        if (th instanceof UnavailableDeviceNotCompatibleException) {
            return true;
        }
        if (th instanceof UnavailableException) {
            String simpleName = th.getClass().getSimpleName();
            if (simpleName.contains("DeviceNotCompatible") || simpleName.contains("Unsupported")) {
                return true;
            }
        }
        String simpleName2 = th.getClass().getSimpleName();
        String lowerCase = String.valueOf(th.getMessage()).toLowerCase();
        if (simpleName2.contains("DeviceNotCompatible") || simpleName2.contains("UnsupportedDevice")) {
            return true;
        }
        return lowerCase.contains("not compatible") || lowerCase.contains("device is not supported") || lowerCase.contains("unsupported_device");
    }

    public static boolean isSupported(Context context) {
        try {
            return check(context).isSupported();
        } catch (Throwable unused) {
            return false;
        }
    }

    public static boolean isTransient(Context context) {
        try {
            return check(context).isTransient();
        } catch (Throwable unused) {
            return false;
        }
    }

    /** Hardware ToF / depth camera feature (nem egyenlő ARCore Depth API-val). */
    public static boolean hasHardwareDepthSensor(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            // Nincs egységes LiDAR feature Androidon; ToF / depth capability hint.
            return pm.hasSystemFeature("android.hardware.camera.capability.depth_output")
                    || pm.hasSystemFeature("android.hardware.sensor.tof");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * ARCore Depth API (AUTOMATIC) támogatott-e. Ideiglenes Session-nel ellenőriz;
     * eredmény cache-elve. Nem állítjuk, hogy LiDAR van, ha nincs támogatás.
     */
    public static synchronized boolean isDepthApiSupported(Context context) {
        if (depthSupportedCache != null) {
            return depthSupportedCache;
        }
        if (isDeviceUnsupported(context)) {
            depthSupportedCache = false;
            return false;
        }
        ArCoreApk.Availability av = check(context);
        if (av == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            depthSupportedCache = false;
            return false;
        }
        if (!av.isSupported() && av != ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD
                && av != ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED) {
            // Transient / unknown — ne cache-eljünk false-t véglegesen
            return false;
        }
        Session session = null;
        try {
            session = new Session(context);
            boolean ok = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            depthSupportedCache = ok;
            Log.i(TAG, "DepthMode.AUTOMATIC supported=" + ok);
            return ok;
        } catch (Throwable th) {
            Log.w(TAG, "Depth probe failed", th);
            // Ne cache-eljünk hibát, ha ARCore még nincs telepítve
            if (av == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED
                    || av == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD
                    || av.isTransient()) {
                return false;
            }
            depthSupportedCache = false;
            return false;
        } finally {
            if (session != null) {
                try { session.close(); } catch (Throwable ignored) {}
            }
        }
    }

    public static synchronized void clearDepthCache() {
        depthSupportedCache = null;
    }
}
