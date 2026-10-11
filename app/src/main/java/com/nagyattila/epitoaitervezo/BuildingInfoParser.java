package com.nagyattila.epitoaitervezo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BuildingInfoParser {
    private static final Pattern AREA = Pattern.compile("(?i)(\\d+[.,]?\\d*)\\s*(?:m²|m2|nm|négyzetméter|sq\\.?\\s*m)");
    private static final Pattern LENGTH = Pattern.compile("(?i)(\\d+[.,]?\\d*)\\s*(mm|cm|m)\\b");
    private static final Pattern INSUL_NEAR_CM = Pattern.compile("(?i)(?:szigetel(?:és|ő)?|insulation|eps|xps|kőzetgyapot|kozetgyapot|poliuretán|poliuretan|pur|pir|hab).{0,40}?(\\d+[.,]?\\d*)\\s*cm|(\\d+[.,]?\\d*)\\s*cm.{0,40}?(?:szigetel(?:és|ő)?|insulation|eps|xps|kőzetgyapot|kozetgyapot|poliuretán|poliuretan|pur|pir|hab)");
    private static final String[][] MATERIAL_MAP = {new String[]{"EPS grafit", "eps grafit", "grafitos eps", "graphite eps"}, new String[]{"EPS fehér", "eps fehér", "eps feher", "white eps", "eps white"}, new String[]{"EPS", "\\beps\\b"}, new String[]{"XPS", "\\bxps\\b"}, new String[]{"Kőzetgyapot", "kőzetgyapot", "kozetgyapot", "rockwool", "rock wool", "ásványgyapot", "asvanygyapot"}, new String[]{"Üveggyapot", "üveggyapot", "uveggyapot", "glasswool", "glass wool"}, new String[]{"PUR/PIR", "poliuretán", "poliuretan", "\\bpur\\b", "\\bpir\\b", "pur/pir"}, new String[]{"Hab", "polisztirol hab", "extrudált hab", "extrudalt hab", "\\bhab\\b"}, new String[]{"Tégla", "tégla", "tegla", "brick"}, new String[]{"Beton", "beton", "concrete"}, new String[]{"Gipszkarton", "gipszkarton", "drywall"}, new String[]{"Fa", "\\bfa\\b", "timber", "wood"}};

    public static final class Dimension {
        public final float meters;
        public final String unit;
        public final float value;

        public Dimension(float f2, String str) {
            this.value = f2;
            this.unit = str;
            if ("mm".equalsIgnoreCase(str)) {
                this.meters = f2 / 1000.0f;
            } else if ("cm".equalsIgnoreCase(str)) {
                this.meters = f2 / 100.0f;
            } else {
                this.meters = f2;
            }
        }

        public String toString() {
            return String.format(Locale.US, "%.2f %s (%.3f m)", Float.valueOf(this.value), this.unit, Float.valueOf(this.meters));
        }
    }

    public static final class Result {
        public float areaM2 = 0.0f;
        public final List<Dimension> dimensions = new ArrayList();
        public final List<String> materials = new ArrayList();
        public String insulationType = null;
        public Float insulationThicknessCm = null;
        public String summary = "";
    }

    private BuildingInfoParser() {
    }

    public static void applyToPlanStore(Result result, String str) {
        int i2 = 1;
        PlanStore planStore = PlanStore.get();
        planStore.setImportedSourceName(str != null ? str : "");
        planStore.setLastImportSummary(result.summary);
        planStore.setImportNotes("automatikus kinyerés — ellenőrizd");
        float f2 = result.areaM2;
        if (f2 > 0.01f) {
            planStore.setMeasuredPolygonAreaM2(f2);
        }
        ArrayList arrayList = new ArrayList();
        Iterator<Dimension> it = result.dimensions.iterator();
        while (it.hasNext()) {
            float f3 = it.next().meters;
            if (f3 >= 0.5f && f3 <= 200.0f) {
                arrayList.add(Float.valueOf(f3));
            }
        }
        java.util.Collections.sort(arrayList);
        if (arrayList.size() >= 2) {
            float floatValue = ((Float) arrayList.get(0)).floatValue();
            float floatValue2 = ((Float) arrayList.get(1)).floatValue();
            if (result.areaM2 > 0.01f) {
                float f4 = Float.MAX_VALUE;
                int i3 = 0;
                while (i3 < arrayList.size()) {
                    int i4 = i3 + 1;
                    for (int i5 = i4; i5 < arrayList.size(); i5++) {
                        float floatValue3 = ((Float) arrayList.get(i3)).floatValue();
                        float floatValue4 = ((Float) arrayList.get(i5)).floatValue();
                        float abs = Math.abs((floatValue3 * floatValue4) - result.areaM2);
                        if (abs < f4) {
                            f4 = abs;
                            floatValue = floatValue3;
                            floatValue2 = floatValue4;
                        }
                    }
                    i3 = i4;
                }
            }
            ArrayList arrayList2 = new ArrayList();
            arrayList2.add(new float[]{0.0f, 0.0f});
            arrayList2.add(new float[]{floatValue, 0.0f});
            arrayList2.add(new float[]{floatValue, floatValue2});
            arrayList2.add(new float[]{0.0f, floatValue2});
            planStore.setFloorVertices(arrayList2);
            if (result.areaM2 < 0.01f) {
                planStore.setMeasuredPolygonAreaM2(floatValue * floatValue2);
            }
        } else {
            float f5 = result.areaM2;
            if (f5 > 0.01f) {
                float sqrt = (float) Math.sqrt(f5);
                ArrayList arrayList3 = new ArrayList();
                arrayList3.add(new float[]{0.0f, 0.0f});
                arrayList3.add(new float[]{sqrt, 0.0f});
                arrayList3.add(new float[]{sqrt, sqrt});
                arrayList3.add(new float[]{0.0f, sqrt});
                planStore.setFloorVertices(arrayList3);
            }
        }
        String str2 = result.insulationType;
        if (str2 != null) {
            planStore.setInsulationType(str2);
        }
        Float f6 = result.insulationThicknessCm;
        if (f6 != null) {
            planStore.setInsulationThicknessCm(f6.floatValue());
        }
    }

    private static String buildSummary(Result result) {
        StringBuilder sb = new StringBuilder("Terület: ");
        if (result.areaM2 > 0.0f) {
            sb.append(String.format(Locale.forLanguageTag("hu-HU"), "%.2f m²", Float.valueOf(result.areaM2)));
        } else {
            sb.append("—");
        }
        sb.append("\nMéretek: ");
        if (result.dimensions.isEmpty()) {
            sb.append("—");
        } else {
            int min = Math.min(12, result.dimensions.size());
            for (int i2 = 0; i2 < min; i2++) {
                if (i2 > 0) {
                    sb.append(", ");
                }
                Dimension dimension = result.dimensions.get(i2);
                sb.append(String.format(Locale.US, "%.2f %s", Float.valueOf(dimension.value), dimension.unit));
            }
            if (result.dimensions.size() > min) {
                sb.append("…");
            }
        }
        sb.append("\nAnyagok: ");
        if (result.materials.isEmpty()) {
            sb.append("—");
        } else {
            sb.append(String.join(", ", result.materials));
        }
        if (result.insulationType != null) {
            sb.append("\nSzigetelés: ");
            sb.append(result.insulationType);
            Float f2 = result.insulationThicknessCm;
            if (f2 != null) {
                sb.append(String.format(Locale.US, " (%.0f cm)", f2));
            }
        }
        return sb.toString();
    }

    private static boolean containsIgnoreCase(String str, String str2) {
        if (str2 == null || str2.isEmpty()) {
            return false;
        }
        return str.contains(str2.toLowerCase(Locale.forLanguageTag("hu-HU")));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int lambda$applyToPlanStore$0(Float f2, Float f3) {
        return Float.compare(f3.floatValue(), f2.floatValue());
    }

    public static Result parse(String str) {
        Result result = new Result();
        if (str == null || str.trim().isEmpty()) {
            result.summary = "Nincs kinyerhető szöveg.";
            return result;
        }
        String replace = str.replace((char) 160, ' ');
        Matcher matcher = AREA.matcher(replace);
        float f2 = 0.0f;
        while (matcher.find()) {
            float parseNum = parseNum(matcher.group(1));
            if (parseNum > f2 && parseNum < 1000000.0f) {
                f2 = parseNum;
            }
        }
        result.areaM2 = f2;
        Matcher matcher2 = LENGTH.matcher(replace);
        while (matcher2.find()) {
            int end = matcher2.end();
            String substring = replace.substring(Math.min(end, replace.length()), Math.min(end + 2, replace.length()));
            if (!"m".equalsIgnoreCase(matcher2.group(2)) || (!substring.startsWith("²") && !substring.toLowerCase(Locale.ROOT).startsWith("2"))) {
                float parseNum2 = parseNum(matcher2.group(1));
                if (parseNum2 > 0.0f) {
                    result.dimensions.add(new Dimension(parseNum2, matcher2.group(2).toLowerCase(Locale.ROOT)));
                }
            }
        }
        String lowerCase = replace.toLowerCase(Locale.forLanguageTag("hu-HU"));
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        for (String[] strArr : MATERIAL_MAP) {
            String str2 = strArr[0];
            for (int i2 = 1; i2 < strArr.length; i2++) {
                if (lowerCase.matches("(?s).*" + strArr[i2] + ".*") || containsIgnoreCase(lowerCase, stripRegex(strArr[i2]))) {
                    linkedHashSet.add(str2);
                    break;
                }
            }
        }
        result.materials.addAll(linkedHashSet);
        if (linkedHashSet.contains("EPS grafit")) {
            result.insulationType = "EPS grafit";
        } else if (linkedHashSet.contains("EPS fehér") || linkedHashSet.contains("EPS")) {
            result.insulationType = linkedHashSet.contains("EPS fehér") ? "EPS fehér" : "EPS grafit";
        } else if (linkedHashSet.contains("XPS")) {
            result.insulationType = "XPS";
        } else if (linkedHashSet.contains("Kőzetgyapot")) {
            result.insulationType = "Kőzetgyapot";
        } else if (linkedHashSet.contains("Üveggyapot")) {
            result.insulationType = "Üveggyapot";
        } else if (linkedHashSet.contains("PUR/PIR") || linkedHashSet.contains("Hab")) {
            result.insulationType = "PUR/PIR";
        }
        Matcher matcher3 = INSUL_NEAR_CM.matcher(replace);
        if (matcher3.find()) {
            String group = matcher3.group(1);
            String group2 = matcher3.group(2);
            if (group == null) {
                group = group2;
            }
            float parseNum3 = parseNum(group);
            if (parseNum3 > 0.0f && parseNum3 < 100.0f) {
                result.insulationThicknessCm = Float.valueOf(parseNum3);
            }
        }
        if (result.insulationThicknessCm == null) {
            Iterator<Dimension> it = result.dimensions.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Dimension next = it.next();
                if ("cm".equals(next.unit)) {
                    float f3 = next.value;
                    if (f3 >= 4.0f && f3 <= 40.0f) {
                        result.insulationThicknessCm = Float.valueOf(f3);
                        break;
                    }
                }
            }
        }
        result.summary = buildSummary(result);
        return result;
    }

    private static float parseNum(String str) {
        if (str == null) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(str.replace(',', '.').trim());
        } catch (Exception unused) {
            return 0.0f;
        }
    }

    private static String stripRegex(String str) {
        return str.replace("\\b", "").replace("\\\\", "");
    }
}
