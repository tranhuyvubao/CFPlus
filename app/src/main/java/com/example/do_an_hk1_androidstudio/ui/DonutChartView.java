package com.example.do_an_hk1_androidstudio.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DonutChartView extends View {

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final List<Segment> segments = new ArrayList<>();

    private float ringWidth;
    private String centerTitle = "";
    private String centerValue = "";

    public DonutChartView(Context context) {
        this(context, null);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        ringWidth = 18f * density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeWidth(ringWidth);
        trackPaint.setColor(0xFFE2E8F0);

        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeCap(Paint.Cap.ROUND);
        segmentPaint.setStrokeWidth(ringWidth);

        titlePaint.setColor(0xFF64748B);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(12f * density);

        valuePaint.setColor(0xFF0F172A);
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(20f * density);
    }

    public void setCenterText(String title, String value) {
        centerTitle = title == null ? "" : title;
        centerValue = value == null ? "" : value;
        invalidate();
    }

    public void setSegments(List<Segment> newSegments) {
        segments.clear();
        if (newSegments != null) {
            segments.addAll(newSegments);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float diameter = Math.min(width, height);
        float padding = ringWidth;
        float left = (width - diameter) / 2f + padding;
        float top = (height - diameter) / 2f + padding;
        float right = (width + diameter) / 2f - padding;
        float bottom = (height + diameter) / 2f - padding;
        arcBounds.set(left, top, right, bottom);

        canvas.drawArc(arcBounds, -90f, 360f, false, trackPaint);

        float total = 0f;
        for (Segment segment : segments) {
            total += Math.max(0, segment.value);
        }

        if (total > 0f) {
            float startAngle = -90f;
            float gapAngle = 3.5f;
            for (Segment segment : segments) {
                if (segment.value <= 0f) {
                    continue;
                }
                float sweep = (segment.value / total) * 360f;
                sweep = Math.max(0f, sweep - gapAngle);
                segmentPaint.setColor(segment.color);
                canvas.drawArc(arcBounds, startAngle, sweep, false, segmentPaint);
                startAngle += sweep + gapAngle;
            }
        }

        float centerX = width / 2f;
        float centerY = height / 2f;
        canvas.drawText(centerTitle, centerX, centerY - titlePaint.descent(), titlePaint);
        canvas.drawText(centerValue, centerX, centerY + valuePaint.getTextSize() * 0.45f, valuePaint);
    }

    public static class Segment {
        public final float value;
        @ColorInt
        public final int color;
        public final String label;

        public Segment(String label, float value, @ColorInt int color) {
            this.label = label == null ? "" : label;
            this.value = value;
            this.color = color;
        }
    }

    public static String formatPercent(int value, int total) {
        if (total <= 0) {
            return "0%";
        }
        return String.format(Locale.getDefault(), "%d%%", Math.round((value * 100f) / total));
    }
}
