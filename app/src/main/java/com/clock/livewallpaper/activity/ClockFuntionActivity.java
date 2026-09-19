package com.clock.livewallpaper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.clock.livewallpaper.R;

/**
 * The three clock sections: Analog, Digital, Smart.
 *
 * <p>This chooser is ad free on purpose. The spec forbids showing an ad merely for opening a clock
 * section, and an ad here would also be the first thing a user meets before seeing any content. Native
 * ads appear only inside the section grids, and the rewarded ad only from the unlock dialog a locked
 * clock offers.
 *
 * <p>The subtitle names the free count of every section; it is the same
 * {@code AdConfig.FREE_CLOCKS_PER_SECTION} the tiles and the unlock gate read, so this screen can never
 * advertise something the gate does not honour.
 */
public class ClockFuntionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_clock_funtion);
        openOnTap(R.id.frameAnalogClock, 0);
        openOnTap(R.id.frameTextClock, 1);
        openOnTap(R.id.frameSmartClock, 2);
    }

    private void openOnTap(int viewId, final int isWhich) {
        FrameLayout frame = (FrameLayout) findViewById(viewId);
        if (frame == null) {
            return;
        }
        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ClockFuntionActivity.this, ClockCardActivity.class);
                intent.putExtra("isWhich", isWhich);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
