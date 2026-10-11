# Építő AI Tervező

Android-prototípus építőipari helyszíni felméréshez és 2D/3D tervezéshez.

**Copyright © Nagy Attila**  
Csomagnév: `com.nagyattila.epitoaitervezo`  
**Verzió: 0.3.10 (versionCode 15)**

## 0.3.10 — 2D szerkesztő (drafting suite)

### Kész (működő mobil UX)
- **Szerkezet:** fal rajzolás, sarok húzás (átméretezés), falosztás, falvastagság, helyiség zárás + név + terület, padló kitöltés (hatch), egyszerű szintválasztó
- **Nyílászárók:** Ajtó (szélesség/magasság + nyitás), Ablak (szélesség/magasság/párkány), Lépcső (fokok + méretek)
- **Katalógus:** kis bútorkönyvtár (ágy, asztal, szék, kanapé, szekrény, mosdó, WC, tűzhely, íróasztal) — forgatás / tükör / méret
- **Pontosság:** rács + snap, végpont/középpont snap, 45°/90° szög-snap, skála kalibráció, m/cm egység
- **Annotáció:** automatikus falméretek, kézi méretvonal, szövegfelirat, padló hatch
- **Szerkesztés:** kijelölés, mozgatás, másol/beilleszt, törlés, undo/redo, rétegek (fal/nyílászáró/bútor/annotáció)
- **Mérés:** hossz / terület / kerület → költség + szigetelés (nyílászáró-levonás)
- **Nézet/export:** 2D szerkesztő, 3D extrudálás nyílászáró jelekkel, PDF+PNG, JSON mentés/betöltés
- **Mérés (0.3.9):** MeasureChooser + AR + Alap mérés megmarad

### Későbbre (nincs a 0.3.10-ben)
Íves falak, teljes DWG/DXF, LiDAR/Bluetooth lézer, nagy ACA blokk-könyvtár, VR séta, IFC.

### Egyszerűsítések
- Egy helyiség / szint egyszerre (multi-room csak egymás utáni falakkal)
- Bútorkatalógus kis szimbólumkészlet (téglalap)
- 3D: izometrikus extrudálás, nem teljes CAD
- Multi-select: elsősorban egy kijelölés + copy

## Fordítás

```bash
export ANDROID_HOME=/workspace/android-sdk
cd epito-ai-tervezo
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/EpitoAITervezo.apk` (arm64-v8a, minify/shrink)
