package com.example.floatingcounter;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.TextView;

public class FloatingService extends Service {
    private WindowManager wm;
    private View bubble;
    private WindowManager.LayoutParams params;
    private int count;
    private android.content.SharedPreferences prefs;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("counter", MODE_PRIVATE);
        count = prefs.getInt("count", 0);
        startForeground(7, makeNotification());
        showBubble();
    }

    private Notification makeNotification() {
        String channel = "floating_counter";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(channel, "Floating Counter",
                NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        return new Notification.Builder(this, channel)
            .setContentTitle("Floating Counter is running")
            .setContentText("Tap the blue bubble to count. Open app to hide it.")
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .build();
    }

    private void showBubble() {
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(count));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(17);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(25, 118, 210));
        bg.setShape(GradientDrawable.OVAL);
        bg.setStroke(2, Color.WHITE);
        tv.setBackground(bg);
        int size = (int)(56 * getResources().getDisplayMetrics().density);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30; params.y = 180;

        tv.setOnClickListener(v -> {
            count++;
            prefs.edit().putInt("count", count).apply();
            tv.setText(String.valueOf(count));
        });

        tv.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float downX, downY; long downTime;
            boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x; startY = params.y;
                    downX = e.getRawX(); downY = e.getRawY();
                    downTime = System.currentTimeMillis(); moved = false;
                    return false;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    int dx = (int)(e.getRawX() - downX), dy = (int)(e.getRawY() - downY);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true;
                    if (moved) {
                        params.x = startX + dx; params.y = startY + dy;
                        wm.updateViewLayout(bubble, params);
                    }
                    return moved;
                } else if (e.getAction() == MotionEvent.ACTION_UP && moved) return true;
                return false;
            }
        });
        bubble = tv;
        wm.addView(bubble, params);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (wm != null && bubble != null) wm.removeView(bubble);
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
