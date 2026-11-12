package com.example.rfdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A deliberately tiny target for rustfrida smoke tests.
 *
 * Every method here is a plain, non-intrinsic, application-level instance or
 * static method so that a hook lands on ordinary ART quick code -- no
 * core-library intrinsics and no system-app weirdness to confound results.
 */
public class MainActivity extends Activity {
    private static final String TAG = "RFDemo";

    private TextView out;
    private final StringBuilder log = new StringBuilder();

    private int counter = 0;
    private String secret = "ORIGINAL_SECRET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        out = new TextView(this);
        out.setTextSize(13f);
        root.addView(out);

        root.addView(button("computeSum()", new Runnable() {
            public void run() { append("computeSum(2,3) = " + computeSum(2, 3)); }
        }));
        root.addView(button("getSecret()", new Runnable() {
            public void run() { append("getSecret() = " + getSecret()); }
        }));
        root.addView(button("instanceMethod()", new Runnable() {
            public void run() { append("instanceMethod(10) = " + instanceMethod(10)); }
        }));
        root.addView(button("tick() x1", new Runnable() {
            public void run() { tick(); append("tick() -> counter=" + counter); }
        }));
        root.addView(button("start ticker (2s)", new Runnable() {
            public void run() { startTicker(); append("ticker started"); }
        }));
        root.addView(button("clear", new Runnable() {
            public void run() { log.setLength(0); render(); }
        }));

        setContentView(scroll);
        append("ready, pid=" + android.os.Process.myPid());

        // Give rfdemo a heartbeat so a hook installed later has something to hit.
        startTicker();
    }

    private Button button(String label, final Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(v -> {
            try {
                action.run();
            } catch (Throwable t) {
                append("EX: " + t);
            }
        });
        return b;
    }

    // ---- hook targets ------------------------------------------------------

    /** Static method returning a plain int. */
    public static int computeSum(int a, int b) {
        return a + b;
    }

    /** Instance method returning a String. */
    public String getSecret() {
        return secret;
    }

    /** Instance method with an argument and arithmetic. */
    public int instanceMethod(int x) {
        return x * 2 + 1;
    }

    /** Called from a background-ish loop; good for watching repeated hits. */
    public void tick() {
        counter++;
    }

    private void startTicker() {
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            public void run() {
                tick();
                Log.i(TAG, "tick counter=" + counter);
                h.postDelayed(this, 2000);
            }
        }, 2000);
    }

    // ---- ui ----------------------------------------------------------------

    private void append(String line) {
        log.append(line).append('\n');
        render();
    }

    private void render() {
        out.setText(log.toString());
    }
}
