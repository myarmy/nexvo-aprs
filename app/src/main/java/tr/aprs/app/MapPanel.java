package tr.aprs.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/* JADX INFO: loaded from: classes3.dex */
final class MapPanel extends View {
    private final Paint paint;
    private AprsFiClient.Station station;

    MapPanel(Context context) {
        super(context);
        this.paint = new Paint(1);
        setContentDescription("APRS istasyon haritası");
    }

    void setStation(AprsFiClient.Station station) {
        this.station = station;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float width;
        float f;
        float height;
        float f2;
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(7, 26, 43));
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(2.0f);
        this.paint.setColor(Color.rgb(32, 62, 82));
        for (int x = -getHeight(); x < getWidth() + getHeight(); x += 70) {
            canvas.drawLine(x, 0.0f, getHeight() + x, getHeight(), this.paint);
        }
        for (int y = 35; y < getHeight(); y += 60) {
            canvas.drawLine(0.0f, y, getWidth(), y - 25, this.paint);
        }
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setTextAlign(Paint.Align.CENTER);
        this.paint.setColor(-3355444);
        this.paint.setTextSize(22.0f);
        canvas.drawText("İSTANBUL", getWidth() / 2.0f, getHeight() / 2.0f, this.paint);
        if (this.station == null) {
            width = getWidth();
            f = 0.62f;
        } else {
            width = getWidth();
            f = 0.58f;
        }
        float cx = width * f;
        if (this.station == null) {
            height = getHeight();
            f2 = 0.42f;
        } else {
            height = getHeight();
            f2 = 0.38f;
        }
        float cy = height * f2;
        this.paint.setColor(Color.rgb(245, 166, 35));
        canvas.drawCircle(cx, cy, 20.0f, this.paint);
        this.paint.setColor(-1);
        canvas.drawCircle(cx, cy, 8.0f, this.paint);
        this.paint.setColor(Color.rgb(245, 166, 35));
        this.paint.setTextSize(18.0f);
        String label = this.station == null ? "TA1ABC-9" : this.station.name;
        canvas.drawText(label, cx, cy - 30.0f, this.paint);
        this.paint.setColor(Color.rgb(24, 166, 184));
        canvas.drawCircle(getWidth() * 0.28f, getHeight() * 0.68f, 16.0f, this.paint);
        this.paint.setColor(-1);
        this.paint.setTextSize(15.0f);
        canvas.drawText("MERKEZ-1", getWidth() * 0.28f, (getHeight() * 0.68f) - 25.0f, this.paint);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(4.0f);
        this.paint.setColor(Color.rgb(245, 166, 35));
        Path route = new Path();
        route.moveTo(getWidth() * 0.28f, getHeight() * 0.68f);
        route.lineTo(cx, cy);
        canvas.drawPath(route, this.paint);
    }
}
