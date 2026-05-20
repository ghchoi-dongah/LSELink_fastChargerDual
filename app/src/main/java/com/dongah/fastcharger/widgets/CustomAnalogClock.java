package com.dongah.fastcharger.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Calendar;

public class CustomAnalogClock extends View {

    private Paint backgroundPaint;
    private Paint hourTickPaint;
    private Paint minuteTickPaint;
    private Paint textPaint;
    private Paint hourHandPaint;
    private Paint minuteHandPaint;
    private Paint secondHandPaint;
    private Paint centerPaint;

    private final Rect textBounds = new Rect();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();                // 다시 그리기 → 시간이 바뀜
            handler.postDelayed(this, 1000); // 1초마다 실행
        }
    };

    public CustomAnalogClock(Context context) {
        super(context);
        init();
    }

    public CustomAnalogClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomAnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 배경 원(그림자 포함)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);

        // 그림자 사용하려면 SW 레이어로
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        backgroundPaint.setShadowLayer(20f, 0f, 0f, Color.argb(128, 0, 0, 0));

        // 시간 눈금 (굵은 눈금)
        hourTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hourTickPaint.setColor(Color.BLACK);
        hourTickPaint.setStrokeWidth(6f);

        // 분 눈금 (얇은 눈금)
        minuteTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minuteTickPaint.setColor(Color.GRAY);
        minuteTickPaint.setStrokeWidth(3f);

        // 숫자(1~12)
//        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        textPaint.setColor(Color.BLACK);
//        textPaint.setTextSize(40f);
//        textPaint.setTextAlign(Paint.Align.CENTER);

        // 시침
        hourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hourHandPaint.setColor(Color.BLACK);
        hourHandPaint.setStrokeWidth(10f);
        hourHandPaint.setStrokeCap(Paint.Cap.ROUND);

        // 분침
        minuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minuteHandPaint.setColor(Color.BLACK);
        minuteHandPaint.setStrokeWidth(8f);
        minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);

        // 초침
        secondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondHandPaint.setColor(Color.RED);
        secondHandPaint.setStrokeWidth(4f);
        secondHandPaint.setStrokeCap(Paint.Cap.ROUND);


        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(Color.BLACK);
        centerPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(ticker);   // 화면에 붙을 때부터 시계 시작
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker); // 메모리 누수 방지
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 20f;  // 여백 조금

        // 1) 배경 원
        canvas.drawCircle(cx, cy, radius, backgroundPaint);

        // 2) 눈금(60개) – 6도 단위로
        for (int i = 0; i < 60; i++) {
            double degree = i * 6.0; // 360 / 60
            double radian = Math.toRadians(degree);

            boolean isHourTick = (i % 5 == 0);

            float tickLength = isHourTick ? 40f : 20f;
            float startRadius = radius - tickLength;
            float endRadius = radius;

            float startX = cx + (float) (startRadius * Math.cos(radian));
            float startY = cy + (float) (startRadius * Math.sin(radian));
            float endX = cx + (float) (endRadius * Math.cos(radian));
            float endY = cy + (float) (endRadius * Math.sin(radian));

            Paint p = isHourTick ? hourTickPaint : minuteTickPaint;
            canvas.drawLine(startX, startY, endX, endY, p);
        }

        // 3) 숫자(1~12) – 각도 30도 단위, 12시는 -90도 방향
//        for (int hour = 1; hour <= 12; hour++) {
//            String text = String.valueOf(hour);
//
//            // 글에서와 같이 각도 기준: 12시가 위, 시계방향
//            double degree = (hour * 30.0) - 90.0;
//            double radian = Math.toRadians(degree);
//
//            float textRadius = radius - 80f; // 눈금보다 안쪽
//            float x = cx + (float) (textRadius * Math.cos(radian));
//            float y = cy + (float) (textRadius * Math.sin(radian));
//
//            textPaint.getTextBounds(text, 0, text.length(), textBounds);
//            float textHeight = textBounds.height();
//
//            canvas.drawText(text, x, y + textHeight / 2f, textPaint);
//        }

        // 4) 현재 시간 받아오기
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);        // 0~11
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        // 5) 시침 각도 계산 (분/초 반영)
        float hourAngle =
                (float) ((hour + minute / 60f + second / 3600f) * 30.0 - 90.0);
        float minuteAngle =
                (float) ((minute + second / 60f) * 6.0 - 90.0);
        float secondAngle =
                (float) (second * 6.0 - 90.0);

        // 6) 시침 그리기
        float hourLength = radius * 0.5f;
        double hourRad = Math.toRadians(hourAngle);
        float hourEndX = cx + (float) (hourLength * Math.cos(hourRad));
        float hourEndY = cy + (float) (hourLength * Math.sin(hourRad));
        canvas.drawLine(cx, cy, hourEndX, hourEndY, hourHandPaint);

        // 7) 분침 그리기
        float minuteLength = radius * 0.7f;
        double minRad = Math.toRadians(minuteAngle);
        float minEndX = cx + (float) (minuteLength * Math.cos(minRad));
        float minEndY = cy + (float) (minuteLength * Math.sin(minRad));
        canvas.drawLine(cx, cy, minEndX, minEndY, minuteHandPaint);

        // 8) 초침 그리기
        float secondLength = radius * 0.8f;
        double secRad = Math.toRadians(secondAngle);
        float secEndX = cx + (float) (secondLength * Math.cos(secRad));
        float secEndY = cy + (float) (secondLength * Math.sin(secRad));
        canvas.drawLine(cx, cy, secEndX, secEndY, secondHandPaint);

        // 9) 중심점
        canvas.drawCircle(cx, cy, 8f, centerPaint);
    }
}
