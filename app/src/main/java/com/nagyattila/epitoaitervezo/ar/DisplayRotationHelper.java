package com.nagyattila.epitoaitervezo.ar;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.WindowManager;
import com.google.ar.core.Session;

public final class DisplayRotationHelper implements DisplayManager.DisplayListener {
    private final Display display;
    private final DisplayManager displayManager;
    private boolean viewportChanged;
    private int viewportHeight;
    private int viewportWidth;

    public DisplayRotationHelper(Context context) {
        this.displayManager = (DisplayManager) context.getSystemService("display");
        this.display = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
    }

    public int getRotation() {
        return this.display.getRotation();
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayAdded(int i2) {
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayChanged(int i2) {
        this.viewportChanged = true;
    }

    @Override // android.hardware.display.DisplayManager.DisplayListener
    public void onDisplayRemoved(int i2) {
    }

    public void onPause() {
        this.displayManager.unregisterDisplayListener(this);
    }

    public void onResume() {
        this.displayManager.registerDisplayListener(this, null);
    }

    public void onSurfaceChanged(int i2, int i3) {
        this.viewportWidth = i2;
        this.viewportHeight = i3;
        this.viewportChanged = true;
    }

    public void updateSessionIfNeeded(Session session) {
        if (this.viewportChanged) {
            session.setDisplayGeometry(this.display.getRotation(), this.viewportWidth, this.viewportHeight);
            this.viewportChanged = false;
        }
    }
}
