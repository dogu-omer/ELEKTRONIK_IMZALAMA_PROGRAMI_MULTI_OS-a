package io.github.doguomer.esign.pades;

import java.util.Locale;

/**
 * Görünür PDF imza alanının yeri. Koordinatlar PDF puanı (1/72 inç) cinsinden ve sayfanın
 * <b>sol üst</b> köşesinden ölçülür. Visible signature box; points from the page's top-left corner.
 *
 * @param page  1'den başlayan sayfa no; 0 = son sayfa / 0 means last page
 * @param x     sol kenardan uzaklık; negatifse sağ alt köşeye yerleşir / negative = bottom-right
 * @param y     üst kenardan uzaklık; negatifse sağ alt köşeye yerleşir
 */
public record VisibleSignature(int page, float x, float y, float width, float height) {

    public static final float WIDTH = 220f;
    public static final float HEIGHT = 64f;
    static final float MARGIN = 36f;

    public VisibleSignature {
        if (page < 0) {
            throw new IllegalArgumentException("Sayfa numarası 0 (son sayfa) veya pozitif olmalı: " + page);
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("İmza kutusu boyutu pozitif olmalı");
        }
    }

    /** Son sayfanın sağ alt köşesi. Bottom-right of the last page. */
    public static VisibleSignature defaults() {
        return new VisibleSignature(0, -1, -1, WIDTH, HEIGHT);
    }

    /**
     * "son" / "" → varsayılan; "3" → 3. sayfa sağ alt; "3,50,700" → 3. sayfa, x=50, y=700.
     */
    public static VisibleSignature parse(String spec) {
        String s = spec == null ? "" : spec.strip().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || s.equals("son") || s.equals("last")) {
            return defaults();
        }
        String[] p = s.split(",");
        try {
            int page = Integer.parseInt(p[0].strip());
            if (p.length == 1) {
                return new VisibleSignature(page, -1, -1, WIDTH, HEIGHT);
            }
            if (p.length == 3) {
                float x = Float.parseFloat(p[1].strip());
                float y = Float.parseFloat(p[2].strip());
                if (x < 0 || y < 0) {
                    throw new IllegalArgumentException("x ve y negatif olamaz: " + spec);
                }
                return new VisibleSignature(page, x, y, WIDTH, HEIGHT);
            }
        } catch (NumberFormatException e) {
            // aşağıda tek mesajla bildirilir
        }
        throw new IllegalArgumentException("Görünür imza konumu 'son', '<sayfa>' veya '<sayfa>,<x>,<y>' olmalı: " + spec);
    }

    boolean automaticPosition() {
        return x < 0 || y < 0;
    }
}
