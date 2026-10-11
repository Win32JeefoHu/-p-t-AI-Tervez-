package com.nagyattila.epitoaitervezo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** OCR PNG/JPG/PDF. Copyright © Nagy Attila */
public final class DocumentTextExtractor {
    private static final int MAX_PDF_OCR_PAGES = 3;
    private static final int OCR_TIMEOUT_SEC = 45;

    private DocumentTextExtractor() {}

    private static Bitmap decodeBitmap(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Kép megnyitása sikertelen");
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            if (bmp == null) throw new IllegalStateException("Kép betöltése sikertelen");
            return scaleDown(bmp, 2000);
        }
    }

    public static String extract(Context context, Uri uri, String mime, String name) throws Exception {
        if (!isSupported(mime, name)) {
            throw new IllegalArgumentException("Csak PNG, JPG/JPEG vagy PDF tölthető fel");
        }
        if (isPdf(mime, name)) return ocrPdfPages(context, uri);
        Bitmap bmp = decodeBitmap(context, uri);
        try {
            return ocrBitmap(bmp);
        } finally {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
    }

    public static boolean isPdf(String mime, String name) {
        if ("application/pdf".equalsIgnoreCase(mime != null ? mime : "")) return true;
        return name != null && name.toLowerCase().endsWith(".pdf");
    }

    public static boolean isSupported(String mime, String name) {
        return isSupportedImage(mime, name) || isPdf(mime, name);
    }

    public static boolean isSupportedImage(String mime, String name) {
        String m = mime != null ? mime.toLowerCase() : "";
        if ("image/png".equals(m) || "image/jpeg".equals(m) || "image/jpg".equals(m)) return true;
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
    }

    private static String ocrBitmap(Bitmap bitmap) throws Exception {
        TextRecognizer client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            Text text = Tasks.await(client.process(InputImage.fromBitmap(bitmap, 0)),
                    OCR_TIMEOUT_SEC, TimeUnit.SECONDS);
            return text.getText() != null ? text.getText() : "";
        } finally {
            client.close();
        }
    }

    private static String ocrPdfPages(Context context, Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return "";
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                int pages = Math.min(renderer.getPageCount(), MAX_PDF_OCR_PAGES);
                TextRecognizer client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                try {
                    for (int i = 0; i < pages; i++) {
                        try (PdfRenderer.Page page = renderer.openPage(i)) {
                            int w = Math.max(1, page.getWidth() * 2);
                            int h = Math.max(1, page.getHeight() * 2);
                            int max = Math.max(w, h);
                            if (max > 2000) {
                                float s = 2000f / max;
                                w = Math.round(w * s);
                                h = Math.round(h * s);
                            }
                            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                            bmp.eraseColor(0xFFFFFFFF);
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            Text text = Tasks.await(client.process(InputImage.fromBitmap(bmp, 0)),
                                    OCR_TIMEOUT_SEC, TimeUnit.SECONDS);
                            if (text.getText() != null) {
                                sb.append(text.getText()).append('\n');
                            }
                            bmp.recycle();
                        }
                    }
                } finally {
                    client.close();
                }
            }
        }
        return sb.toString();
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxSide) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int max = Math.max(w, h);
        if (max <= maxSide) return bitmap;
        float s = maxSide / (float) max;
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.round(w * s), Math.round(h * s), true);
        if (scaled != bitmap) bitmap.recycle();
        return scaled;
    }
}
