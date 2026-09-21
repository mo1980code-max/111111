package com.clock.livewallpaper.activity;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.InputDeviceCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clock.livewallpaper.R;
import com.clock.livewallpaper.ads.AdPolicy;
import com.clock.livewallpaper.clock.ClockPreferences;
import com.clock.livewallpaper.image.LocalImage;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import com.clock.livewallpaper.LiveClockWallpaper;
import com.clock.livewallpaper.adapter.BgAdapter;
import com.clock.livewallpaper.model.Clocks;
import com.clock.livewallpaper.utils.RealPathUtil;
import com.clock.livewallpaper.utils.TinyDB;
import com.clock.livewallpaper.viewUtils.AnalogClock;
import com.clock.livewallpaper.viewUtils.SmartClockPreview;
import com.clock.livewallpaper.viewUtils.TextClockPreview;

import java.io.File;
import java.util.ArrayList;



public class EditorActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int SELECT_PICTURE = 1;
    public static boolean isDone = false;
    private AnalogClock analogClock;
    private BgAdapter bgAdapter;
    private RecyclerView bgRecyclerView;
    private Button btnColor1;
    private Button btnColor2;
    private ImageView btnOk;
    private ImageView icDone;
    private ImageView ivBackground;
    private ImageView ivCenter;
    private ImageView ivColor;
    private ImageView ivGallery;
    private ImageView ivTextColor;
    private ImageView ivZoomIn;
    private ImageView ivZoomOut;
    private LinearLayout layoutBottom;
    private LinearLayout layoutColor;
    private int mClockSize;
    private int mHeight;
    private ImageView mIvMainScreen;
    public int mWidth;
    private AppCompatSeekBar seekBar;
    private SmartClockPreview smartClockPreview;
    private TextClockPreview textClockPreview;
    TinyDB tinyDB;
    String[] permissions = {"android.permission.READ_EXTERNAL_STORAGE"};
    private float mClockPosX = 100.0f;
    private float mClockPosY = 100.0f;
    private int textClockPosition = 0;
    private boolean mIsCenterLine = false;
    private int color1 = -1;
    private int color2 = InputDeviceCompat.SOURCE_ANY;

    @Override

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.activity_editor);
        isDone = false;
        this.tinyDB = new TinyDB(this);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.mWidth = displayMetrics.widthPixels;
        this.mHeight = displayMetrics.heightPixels;
        initView();
    }

    @Override
    public void onBackPressed() {

        EditorActivity.this.finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
        hideMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideMenu();
    }

    public void hideMenu() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }

    private void initView() {
        this.analogClock = (AnalogClock) findViewById(R.id.analogClock);
        this.textClockPreview = (TextClockPreview) findViewById(R.id.textClockPreview);
        this.smartClockPreview = (SmartClockPreview) findViewById(R.id.smartClockPreview);
        this.bgRecyclerView = (RecyclerView) findViewById(R.id.bgRecyclerView);
        this.layoutBottom = (LinearLayout) findViewById(R.id.layoutBottom);
        this.mIvMainScreen = (ImageView) findViewById(R.id.imageView);
        this.ivColor = (ImageView) findViewById(R.id.ivColor);
        this.ivGallery = (ImageView) findViewById(R.id.ivGallery);
        this.ivBackground = (ImageView) findViewById(R.id.ivBackground);
        this.btnOk = (ImageView) findViewById(R.id.btn_ok);
        this.ivZoomOut = (ImageView) findViewById(R.id.ivZoomOut);
        this.ivTextColor = (ImageView) findViewById(R.id.ivTextColor);
        this.ivZoomIn = (ImageView) findViewById(R.id.ivZoomIn);
        this.ivCenter = (ImageView) findViewById(R.id.ivCenter);
        this.icDone = (ImageView) findViewById(R.id.icDone);
        this.layoutColor = (LinearLayout) findViewById(R.id.layoutColor);
        this.btnColor1 = (Button) findViewById(R.id.btnColor1);
        this.btnColor2 = (Button) findViewById(R.id.btnColor2);
        this.seekBar = (AppCompatSeekBar) findViewById(R.id.seekBar);
        this.analogClock.setAutoUpdate(true);
        this.bgRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        getUserSettings();
        this.ivZoomOut.setOnClickListener(this);
        this.ivZoomIn.setOnClickListener(this);
        this.ivColor.setOnClickListener(this);
        this.ivCenter.setOnClickListener(this);
        this.ivGallery.setOnClickListener(this);
        this.ivBackground.setOnClickListener(this);
        this.ivTextColor.setOnClickListener(this);
        this.btnColor1.setOnClickListener(this);
        this.btnColor2.setOnClickListener(this);
        this.icDone.setOnClickListener(this);
        this.btnOk.setOnClickListener(this);
        View liveWallpaperButton = findViewById(R.id.btnSetLiveWallpaper);
        if (liveWallpaperButton != null) {
            liveWallpaperButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveUserSettings();
                    launchLiveWallpaper();
                }
            });
        }
        BgAdapter bgAdapter = new BgAdapter();
        this.bgAdapter = bgAdapter;
        this.bgRecyclerView.setAdapter(bgAdapter);
        this.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                EditorActivity.this.mClockSize = i;
                EditorActivity.this.updateClock();
            }
        });
        this.mIvMainScreen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != 2) {
                    return false;
                }
                float f = (float) ((int) (EditorActivity.this.mClockPosX + ((float) (EditorActivity.this.mClockSize / 2))));
                float f2 = (float) ((int) (EditorActivity.this.mClockPosY - ((float) (EditorActivity.this.mClockSize / 2))));
                float f3 = (float) ((int) (EditorActivity.this.mClockPosY + ((float) (EditorActivity.this.mClockSize / 2))));
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if (x <= ((float) ((int) (EditorActivity.this.mClockPosX - ((float) (EditorActivity.this.mClockSize / 2))))) || x >= f || y <= f2 || y >= f3) {
                    return false;
                }
                if (EditorActivity.this.mIsCenterLine) {
                    EditorActivity editorActivity = EditorActivity.this;
                    editorActivity.mClockPosX = ((float) editorActivity.mWidth) / 2.0f;
                } else {
                    EditorActivity.this.mClockPosX = motionEvent.getX();
                }
                EditorActivity.this.mClockPosY = motionEvent.getY();
                EditorActivity.this.updateClock();
                return false;
            }
        });
        this.bgAdapter.setClickListener(new BgAdapter.ClickListener() {
            @Override
            public void setClick(int i) {
                EditorActivity.this.tinyDB.putInt("customBg", i);
                EditorActivity.this.tinyDB.putBoolean("isImage", false);
                EditorActivity.this.tinyDB.putBoolean("isCustomBg", true);
                EditorActivity.this.updateClock();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnColor1:
                ColorPickerDialogBuilder.with(this).setTitle(R.string.clock_studio_choose_color).wheelType(ColorPickerView.WHEEL_TYPE.FLOWER).density(12).setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int i) {
                    }
                }).setPositiveButton(R.string.clock_studio_ok, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, Integer[] numArr) {
                        EditorActivity.this.hideMenu();
                        EditorActivity.this.color1 = i;
                        EditorActivity.this.updateClock();
                    }
                }).setNegativeButton(R.string.clock_studio_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditorActivity.this.hideMenu();
                    }
                }).build().show();
                return;
            case R.id.btnColor2:
                ColorPickerDialogBuilder.with(this).setTitle(R.string.clock_studio_choose_color).wheelType(ColorPickerView.WHEEL_TYPE.FLOWER).density(12).setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int i) {
                    }
                }).setPositiveButton(R.string.clock_studio_ok, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, Integer[] numArr) {
                        EditorActivity.this.hideMenu();
                        EditorActivity.this.color2 = i;
                        EditorActivity.this.updateClock();
                    }
                }).setNegativeButton(R.string.clock_studio_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditorActivity.this.hideMenu();
                    }
                }).build().show();
                return;
            case R.id.btn_ok:
                this.bgRecyclerView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.VISIBLE);
                saveUserSettings();
                EditorActivity.isDone = true;
                launchLiveWallpaper();
                return;
            case R.id.icDone:
                this.tinyDB.putInt("textColor1", this.color1);
                this.tinyDB.putInt("textColor2", this.color2);
                saveUserSettings();
                this.bgRecyclerView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.GONE);
                return;
            case R.id.ivBackground:
                this.bgRecyclerView.setVisibility(View.VISIBLE);
                this.seekBar.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.GONE);
                return;
            case R.id.ivCenter:
                this.bgRecyclerView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.GONE);
                this.mIsCenterLine = !this.mIsCenterLine;
                this.mClockPosX = ((float) this.mWidth) / 2.0f;
                updateClock();
                return;
            case R.id.ivColor:
                this.bgRecyclerView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.GONE);
                ColorPickerDialogBuilder.with(this).setTitle(R.string.clock_studio_choose_color).wheelType(ColorPickerView.WHEEL_TYPE.FLOWER).density(12).setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int i) {
                    }
                }).setPositiveButton(R.string.clock_studio_ok, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, Integer[] numArr) {
                        EditorActivity.this.hideMenu();
                        EditorActivity.this.tinyDB.putBoolean("isImage", false);
                        EditorActivity.this.tinyDB.putBoolean("isCustomBg", false);
                        EditorActivity.this.tinyDB.putInt("bgColor", i);
                        EditorActivity.this.updateClock();
                    }
                }).setNegativeButton(R.string.clock_studio_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditorActivity.this.hideMenu();
                    }
                }).build().show();
                return;
            case R.id.ivGallery:
                galleryIntent();
                this.bgRecyclerView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.GONE);
                return;
            case R.id.ivTextColor:
                this.layoutColor.setVisibility(View.VISIBLE);
                this.bgRecyclerView.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.GONE);
                return;
            case R.id.ivZoomIn:
            case R.id.ivZoomOut:
                this.bgRecyclerView.setVisibility(View.GONE);
                this.layoutColor.setVisibility(View.GONE);
                this.seekBar.setVisibility(View.VISIBLE);
                return;
            default:
                return;
        }
    }

    public void saveUserSettings() {
        this.tinyDB.putFloat("prefClockPosX", this.mClockPosX);
        this.tinyDB.putFloat("prefClockPosY", this.mClockPosY);
        this.tinyDB.putInt("prefSize", this.mClockSize);
        this.tinyDB.putInt("textClockPosition", this.textClockPosition);
        this.tinyDB.putInt("textColor1", this.color1);
        this.tinyDB.putInt("textColor2", this.color2);
        ClockPreferences.get(this).saveEditorState(this.mClockPosX, this.mClockPosY,
                this.mWidth, this.mHeight, this.mClockSize, this.textClockPosition,
                this.color1, this.color2);
        updateClock();
    }

    private void getUserSettings() {
        this.mClockPosX = this.tinyDB.getFloat("prefClockPosX", ((float) this.mWidth) / 2.0f);
        this.mClockPosY = this.tinyDB.getFloat("prefClockPosY", ((float) this.mHeight) / 2.0f);
        int i = this.tinyDB.getInt("prefSize");
        this.mClockSize = i == 0 ? 500 : Math.max(120, Math.min(1000, i));
        this.seekBar.setProgress(this.mClockSize);
        this.textClockPosition = Math.max(0, this.tinyDB.getInt("textClockPosition"));
        this.color1 = this.tinyDB.getInt("textColor1", -1);
        this.color2 = this.tinyDB.getInt("textColor2", InputDeviceCompat.SOURCE_ANY);
        int clockType = this.tinyDB.getInt("clockType");
        if (clockType < 0 || clockType > 2) {
            clockType = 0;
            this.tinyDB.putInt("clockType", clockType);
        }
        if (clockType == 0) {
            this.analogClock.setVisibility(View.VISIBLE);
            this.textClockPreview.setVisibility(View.GONE);
            this.smartClockPreview.setVisibility(View.GONE);
            this.ivTextColor.setVisibility(View.GONE);
        } else if (this.tinyDB.getInt("clockType") == 1) {
            this.analogClock.setVisibility(View.GONE);
            this.textClockPreview.setVisibility(View.GONE);
            this.smartClockPreview.setVisibility(View.VISIBLE);
            this.ivTextColor.setVisibility(View.GONE);
        } else if (this.tinyDB.getInt("clockType") == 2) {
            this.analogClock.setVisibility(View.GONE);
            this.textClockPreview.setVisibility(View.VISIBLE);
            this.smartClockPreview.setVisibility(View.GONE);
        }
        saveUserSettings();
    }

    public void updateClock() {
        if (this.tinyDB.getInt("clockType") == 0) {
            this.analogClock.setClock((Clocks) this.tinyDB.getObject("clocks", Clocks.class));
            this.analogClock.setClockSize((float) this.mClockSize);
            this.analogClock.setTouchEnable(true);
            this.analogClock.setPosition(this.mClockPosX, this.mClockPosY);
        } else if (this.tinyDB.getInt("clockType") == 1) {
            this.smartClockPreview.setTextClockPosition(this.textClockPosition);
            this.smartClockPreview.config(this.mClockPosX, this.mClockPosY, (int) (((float) this.mClockSize) * 2.0f));
        } else if (this.tinyDB.getInt("clockType") == 2) {
            this.textClockPreview.setTextClockPosition(this.textClockPosition);
            this.textClockPreview.setColors(this.color1, this.color2);
            this.textClockPreview.config(this.mClockPosX, this.mClockPosY, (int) (((float) this.mClockSize) * 2.0f));
        } else {
            // Corrupted preferences must never leave the editor or wallpaper without a renderer.
            this.tinyDB.putInt("clockType", 0);
            this.analogClock.setClock((Clocks) this.tinyDB.getObject("clocks", Clocks.class));
            this.analogClock.setClockSize((float) this.mClockSize);
            this.analogClock.setPosition(this.mClockPosX, this.mClockPosY);
        }
        this.mIvMainScreen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (this.tinyDB.getBoolean("isImage")) {
            // The picked file is on the device already: decode it here, no image loader and no network.
            LocalImage.fromFile(this.mIvMainScreen, new File(this.tinyDB.getString("ImageString")),
                    getResources().getDisplayMetrics().heightPixels);
        } else if (this.tinyDB.getBoolean("isCustomBg")) {
            this.mIvMainScreen.setImageResource(this.tinyDB.getInt("customBg"));
        } else {
            this.mIvMainScreen.setImageResource(0);
            this.mIvMainScreen.setBackgroundColor(this.tinyDB.getInt("bgColor"));
        }
    }

    /**
     * Hands the saved composition to Android's own wallpaper confirmation UI.  The Activity never
     * calls WallpaperManager.setStream/setBitmap, so it cannot silently replace the user's wallpaper.
     */
    private void launchLiveWallpaper() {
        ClockPreferences preferences = ClockPreferences.get(this);
        // load() applies the same free/unlocked-name guard used by the service before anything leaves
        // the app. A direct service launch therefore cannot turn a locked name into a wallpaper.
        preferences.load();
        ComponentName component = new ComponentName(this, LiveClockWallpaper.class);
        Intent request = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        request.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
        AdPolicy.markSystemHandoff();
        try {
            startActivity(request);
            finish();
        } catch (ActivityNotFoundException unavailable) {
            // A few vendor builds omit the direct preview action. Their supported chooser can still
            // show the normal confirmation flow; if it is missing, keep the user in the editor.
            try {
                Intent chooser = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                chooser.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
                startActivity(chooser);
                finish();
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.clock_studio_wallpaper_unavailable,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkPermissions() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            return true;
        }else{
            ArrayList arrayList = new ArrayList();
            String[] strArr = this.permissions;
            for (String str : strArr) {
                if (ContextCompat.checkSelfPermission(this, str) != 0) {
                    arrayList.add(str);
                }
            }
            if (arrayList.isEmpty()) {
                return true;
            }
            ActivityCompat.requestPermissions(this, (String[]) arrayList.toArray(new String[arrayList.size()]), 100);
            return false;
        }

    }

    public void galleryIntent() {
        if (checkPermissions()) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction("android.intent.action.GET_CONTENT");
            AdPolicy.markSystemHandoff();
            startActivityForResult(Intent.createChooser(intent, getString(R.string.clock_studio_select_picture)), 1);
        }
    }

    @Override

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (iArr.length > 0 && iArr[0] == 0) {
            galleryIntent();
        }
    }

    @Override

    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1 && i2 == -1 && intent != null && intent.getData() != null) {
            String realPath = RealPathUtil.getRealPath(this, intent.getData());
            if (realPath != null && !realPath.isEmpty()) {
                this.tinyDB.putBoolean("isImage", true);
                this.tinyDB.putBoolean("isCustomBg", false);
                this.tinyDB.putString("ImageString", realPath);
                updateClock();
            }
        }
    }
}
