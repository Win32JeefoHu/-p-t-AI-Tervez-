package com.nagyattila.epitoaitervezo;

import java.util.ArrayList;
import java.util.List;

public final class PlanStore {
    private static final PlanStore INSTANCE = new PlanStore();
    private float floorPlanAreaM2;
    private float measuredPolygonAreaM2;
    private final List<float[]> floorVertices = new ArrayList();
    private float insulationThicknessCm = 10.0f;
    private String insulationType = "EPS grafit";
    private float insulationWasteFactor = 1.1f;
    private float unitPriceInsulationHuf = 4500.0f;
    private float unitPriceLaborHuf = 3500.0f;
    private String lastImportSummary = "";
    private String importedSourceName = "";
    private String importNotes = "";

    private PlanStore() {
    }

    
    private final java.util.List<BuildingElement> elements = new java.util.ArrayList<>();
    private float unitPriceDoorHuf = 85000f;
    private float unitPriceWindowHuf = 65000f;
    private float unitPriceStairPerMHuf = 45000f;
    private float unitPriceFlooringPerM2Huf = 8500f;
    private float paintCoats = 3f;
    private float paintCoverageLPerM2 = 0.13f;
    private float plasterMm = 10f;
    private float tileWallFactor = 2.5f;
    private float adhesiveKgPerM2 = 3f;
    private float groutKgPerM2 = 0.5f;
    private String lastMaterialsSummary = "";
    private float lastPaintLiters = 0f;
    private float lastPlasterKg = 0f;
    private float unitPricePaintHufPerL = 2500f;
    private float unitPricePlasterHufPerKg = 150f;

    public static PlanStore get() {
        return INSTANCE;
    }

    public static float polygonArea(List<float[]> list) {
        int size = list == null ? 0 : list.size();
        if (size < 3) {
            return 0.0f;
        }
        double d2 = 0.0d;
        int i2 = 0;
        while (i2 < size) {
            float[] fArr = list.get(i2);
            i2++;
            float[] fArr2 = list.get(i2 % size);
            d2 += (fArr[0] * fArr2[1]) - (fArr2[0] * fArr[1]);
        }
        return (float) (Math.abs(d2) * 0.5d);
    }

    public synchronized void clearFloor() {
        this.floorVertices.clear();
        this.floorPlanAreaM2 = 0.0f;
    }

    public synchronized float getBestAreaM2() {
        float f2 = this.floorPlanAreaM2;
        if (f2 > 0.01f) {
            return f2;
        }
        return this.measuredPolygonAreaM2;
    }

    public synchronized float getFloorPlanAreaM2() {
        return this.floorPlanAreaM2;
    }

    public synchronized List<float[]> getFloorVertices() {
        ArrayList arrayList;
        arrayList = new ArrayList(this.floorVertices.size());
        for (float[] fArr : this.floorVertices) {
            arrayList.add(new float[]{fArr[0], fArr[1]});
        }
        return arrayList;
    }

    public synchronized String getImportNotes() {
        return this.importNotes;
    }

    public synchronized String getImportedSourceName() {
        return this.importedSourceName;
    }

    public synchronized float getInsulationThicknessCm() {
        return this.insulationThicknessCm;
    }

    public synchronized String getInsulationType() {
        return this.insulationType;
    }

    public synchronized float getInsulationWasteFactor() {
        return this.insulationWasteFactor;
    }

    public synchronized String getLastImportSummary() {
        return this.lastImportSummary;
    }

    public synchronized float getMeasuredPolygonAreaM2() {
        return this.measuredPolygonAreaM2;
    }

    public synchronized float getUnitPriceInsulationHuf() {
        return this.unitPriceInsulationHuf;
    }

    public synchronized float getUnitPriceLaborHuf() {
        return this.unitPriceLaborHuf;
    }

    public synchronized void setFloorVertices(List<float[]> list) {
        try {
            this.floorVertices.clear();
            if (list != null) {
                for (float[] fArr : list) {
                    if (fArr != null && fArr.length >= 2) {
                        this.floorVertices.add(new float[]{fArr[0], fArr[1]});
                    }
                }
            }
            this.floorPlanAreaM2 = polygonArea(this.floorVertices);
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized void setImportNotes(String str) {
        if (str == null) {
            str = "";
        }
        this.importNotes = str;
    }

    public synchronized void setImportedSourceName(String str) {
        if (str == null) {
            str = "";
        }
        this.importedSourceName = str;
    }

    public synchronized void setInsulationThicknessCm(float f2) {
        this.insulationThicknessCm = f2;
    }

    public synchronized void setInsulationType(String str) {
        if (str != null) {
            if (!str.isEmpty()) {
                this.insulationType = str;
            }
        }
    }

    public synchronized void setInsulationWasteFactor(float f2) {
        this.insulationWasteFactor = Math.max(1.0f, f2);
    }

    public synchronized void setLastImportSummary(String str) {
        if (str == null) {
            str = "";
        }
        this.lastImportSummary = str;
    }

    public synchronized void setMeasuredPolygonAreaM2(float f2) {
        this.measuredPolygonAreaM2 = Math.max(0.0f, f2);
    }

    public synchronized void setUnitPriceInsulationHuf(float f2) {
        this.unitPriceInsulationHuf = Math.max(0.0f, f2);
    }

    public synchronized void setUnitPriceLaborHuf(float f2) {
        this.unitPriceLaborHuf = Math.max(0.0f, f2);
    }

    public synchronized java.util.List<BuildingElement> getElements() {
        return new java.util.ArrayList<>(elements);
    }
    public synchronized void setElements(java.util.List<BuildingElement> list) {
        elements.clear(); if (list != null) elements.addAll(list);
    }
    public synchronized void addElement(BuildingElement e) { if (e != null) elements.add(e); }
    public synchronized boolean removeElement(String id) {
        java.util.Iterator<BuildingElement> it = elements.iterator();
        while (it.hasNext()) { if (id.equals(it.next().id)) { it.remove(); return true; } }
        return false;
    }
    public synchronized void clearElements() { elements.clear(); }
    public synchronized int countByType(BuildingElement.Type type) {
        int n=0; for (BuildingElement e: elements) if (e.type==type) n++; return n;
    }
    public synchronized float flooringAreaM2() {
        for (BuildingElement e: elements) if (e.type==BuildingElement.Type.FLOORING) return getBestAreaM2();
        return 0f;
    }
    public synchronized float stairRunLengthM() {
        float s=0; for (BuildingElement e: elements) if (e.type==BuildingElement.Type.STAIR)
            s += Math.max(1,e.stepCount)*Math.max(0.1f,e.stepDepth); return s;
    }
    public synchronized float getUnitPriceDoorHuf(){return unitPriceDoorHuf;}
    public synchronized void setUnitPriceDoorHuf(float v){unitPriceDoorHuf=Math.max(0,v);}
    public synchronized float getUnitPriceWindowHuf(){return unitPriceWindowHuf;}
    public synchronized void setUnitPriceWindowHuf(float v){unitPriceWindowHuf=Math.max(0,v);}
    public synchronized float getUnitPriceStairPerMHuf(){return unitPriceStairPerMHuf;}
    public synchronized void setUnitPriceStairPerMHuf(float v){unitPriceStairPerMHuf=Math.max(0,v);}
    public synchronized float getUnitPriceFlooringPerM2Huf(){return unitPriceFlooringPerM2Huf;}
    public synchronized void setUnitPriceFlooringPerM2Huf(float v){unitPriceFlooringPerM2Huf=Math.max(0,v);}

    public synchronized float getPaintCoats() { return paintCoats; }
    public synchronized void setPaintCoats(float v) { paintCoats = Math.max(1f, v); }
    public synchronized float getPaintCoverageLPerM2() { return paintCoverageLPerM2; }
    public synchronized void setPaintCoverageLPerM2(float v) { paintCoverageLPerM2 = Math.max(0.01f, v); }
    public synchronized float getPlasterMm() { return plasterMm; }
    public synchronized void setPlasterMm(float v) { plasterMm = Math.max(0.5f, v); }
    public synchronized float getTileWallFactor() { return tileWallFactor; }
    public synchronized void setTileWallFactor(float v) { tileWallFactor = Math.max(0.5f, v); }
    public synchronized float getAdhesiveKgPerM2() { return adhesiveKgPerM2; }
    public synchronized void setAdhesiveKgPerM2(float v) { adhesiveKgPerM2 = Math.max(0.1f, v); }
    public synchronized float getGroutKgPerM2() { return groutKgPerM2; }
    public synchronized void setGroutKgPerM2(float v) { groutKgPerM2 = Math.max(0.05f, v); }
    public synchronized String getLastMaterialsSummary() { return lastMaterialsSummary; }
    public synchronized void setLastMaterialsSummary(String s) { lastMaterialsSummary = s == null ? "" : s; }
    public synchronized float getLastPaintLiters() { return lastPaintLiters; }
    public synchronized void setLastPaintLiters(float v) { lastPaintLiters = Math.max(0f, v); }
    public synchronized float getLastPlasterKg() { return lastPlasterKg; }
    public synchronized void setLastPlasterKg(float v) { lastPlasterKg = Math.max(0f, v); }
    public synchronized float getUnitPricePaintHufPerL() { return unitPricePaintHufPerL; }
    public synchronized void setUnitPricePaintHufPerL(float v) { unitPricePaintHufPerL = Math.max(0f, v); }
    public synchronized float getUnitPricePlasterHufPerKg() { return unitPricePlasterHufPerKg; }
    public synchronized void setUnitPricePlasterHufPerKg(float v) { unitPricePlasterHufPerKg = Math.max(0f, v); }

}
