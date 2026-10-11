package com.nagyattila.epitoaitervezo;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Építőelem: nyílászáró / lépcső / padló. Copyright © Nagy Attila */
public class BuildingElement implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { DOOR, WINDOW, STAIR, FLOORING }

    public String id = UUID.randomUUID().toString();
    public Type type = Type.DOOR;
    public float x, y;
    public int wallIndex = -1;
    public float tAlongWall = 0.5f;
    public float width = 0.9f;
    public float height = 2.1f;
    public float sillHeight = 0.9f;
    public int stepCount = 12;
    public float stepDepth = 0.28f;
    public float stepRise = 0.17f;
    public String material = "";

    public BuildingElement() {}

    public BuildingElement(Type t) {
        this.type = t;
        applyDefaults(t);
    }

    public void applyDefaults(Type t) {
        this.type = t;
        switch (t) {
            case DOOR:
                width = 0.90f; height = 2.10f; sillHeight = 0f; material = "Fa / MDF"; break;
            case WINDOW:
                width = 1.20f; height = 1.40f; sillHeight = 0.90f; material = "Műanyag / PVC"; break;
            case STAIR:
                width = 1.00f; height = 0f; stepCount = 12; stepDepth = 0.28f; stepRise = 0.17f;
                material = "Vasbeton"; break;
            case FLOORING:
                width = 0f; height = 0f; material = "Laminált padló"; break;
        }
    }

    public String typeLabelHu() {
        switch (type) {
            case DOOR: return "Ajtó";
            case WINDOW: return "Ablak";
            case STAIR: return "Lépcső";
            case FLOORING: return "Padló";
            default: return "?";
        }
    }

    public boolean isOpening() {
        return type == Type.DOOR || type == Type.WINDOW;
    }

    public String summaryLine() {
        Locale hu = Locale.forLanguageTag("hu-HU");
        switch (type) {
            case DOOR:
                return String.format(hu, "Ajtó %.2f×%.2f m • %s", width, height, material);
            case WINDOW:
                return String.format(hu, "Ablak %.2f×%.2f m • párkány %.2f m • %s",
                        width, height, sillHeight, material);
            case STAIR:
                return String.format(hu, "Lépcső %d fok • %.2f×%.2f m • %s",
                        stepCount, stepDepth, stepRise, material);
            case FLOORING:
                return String.format(hu, "Padló • %s", material);
            default:
                return typeLabelHu();
        }
    }

    public float[] resolvePosition(List<float[]> floorVerts) {
        if (isOpening() && wallIndex >= 0 && floorVerts != null && floorVerts.size() >= 2) {
            int n = floorVerts.size();
            int i = ((wallIndex % n) + n) % n;
            float[] a = floorVerts.get(i);
            float[] b = floorVerts.get((i + 1) % n);
            float t = Math.max(0f, Math.min(1f, tAlongWall));
            return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
        }
        return new float[]{x, y};
    }
}
