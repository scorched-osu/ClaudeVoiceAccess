package com.claude.voiceaccess.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;

import com.claude.voiceaccess.R;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private Button btnPrevious, btnContinue;
    private LinearLayout dotsLayout;

    // ── Page data ────────────────────────────────────────────────────────────
    static final String[] TITLES = {
        "Learn the basics",
        "Learn icon names",
        "Type and edit text",
        "Grid selection",
        "Grid selection practice",
        "Get more help"
    };

    static final String[] BODIES = {
        "Say \"Hey Claude\" to activate. Then speak a command.\n\n" +
        "Try saying:\n• \"Go home\"\n• \"Show numbers\"\n• \"Scroll down\"\n• \"Open Chrome\"",

        "Claude Voice Access can describe on-screen icons.\n\n" +
        "Say \"Show labels\" to see icon names overlaid on your screen, " +
        "then say the label or number to interact.",

        "Tap any text field, then say \"Type [your text]\".\n\n" +
        "You can also say:\n• \"Delete word\"\n• \"Replace X with Y\"\n• \"Go to beginning\"\n• \"Select all\"",

        "Say \"Show grid\" to overlay a numbered grid on your screen.\n\n" +
        "The grid lets you tap precise areas even without visible labels.",

        "Practice: Say \"Show grid\" and then a number to interact.\n\n" +
        "Say \"More squares\" for a finer grid, or \"Fewer squares\" to simplify.",

        "Say \"All commands\" at any time to browse the full command list.\n\n" +
        "Say \"Help\" for context-sensitive help, or visit Voice Access settings."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        pager       = findViewById(R.id.tutorial_pager);
        btnPrevious = findViewById(R.id.btn_previous);
        btnContinue = findViewById(R.id.btn_continue);
        dotsLayout  = findViewById(R.id.dots_layout);

        ImageButton btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());

        pager.setAdapter(new TutorialAdapter());
        buildDots();
        updateButtons(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                updateButtons(pos);
                updateDots(pos);
            }
        });

        btnPrevious.setOnClickListener(v -> {
            int cur = pager.getCurrentItem();
            if (cur > 0) pager.setCurrentItem(cur - 1);
        });

        btnContinue.setOnClickListener(v -> {
            int cur = pager.getCurrentItem();
            if (cur < TITLES.length - 1) {
                pager.setCurrentItem(cur + 1);
            } else {
                finish();
            }
        });
    }

    private void buildDots() {
        dotsLayout.removeAllViews();
        for (int i = 0; i < TITLES.length; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(16, 16);
            lp.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(android.R.drawable.presence_invisible);
            dotsLayout.addView(dot);
        }
        updateDots(0);
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setColorFilter(i == selected ?
                getResources().getColor(R.color.primary, getTheme()) :
                getResources().getColor(R.color.text_secondary, getTheme()));
        }
    }

    private void updateButtons(int pos) {
        btnPrevious.setVisibility(pos > 0 ? View.VISIBLE : View.INVISIBLE);
        btnContinue.setText(pos == TITLES.length - 1 ? "Finish" : "Continue");
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    class TutorialAdapter extends FragmentStateAdapter {
        TutorialAdapter() { super(TutorialActivity.this); }
        @Override public int getItemCount() { return TITLES.length; }
        @NonNull @Override
        public Fragment createFragment(int pos) {
            return TutorialPageFragment.newInstance(pos);
        }
    }

    // ── Fragment ─────────────────────────────────────────────────────────────

    public static class TutorialPageFragment extends Fragment {
        private static final String ARG_POS = "pos";

        public static TutorialPageFragment newInstance(int pos) {
            TutorialPageFragment f = new TutorialPageFragment();
            Bundle b = new Bundle();
            b.putInt(ARG_POS, pos);
            f.setArguments(b);
            return f;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.item_tutorial_page, container, false);
            int pos = getArguments() != null ? getArguments().getInt(ARG_POS, 0) : 0;

            TextView title = v.findViewById(R.id.tutorial_title);
            TextView body  = v.findViewById(R.id.tutorial_body);

            title.setText(TITLES[pos]);
            body.setText(BODIES[pos]);
            return v;
        }
    }
}
