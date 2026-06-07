package com.claude.voiceaccess.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.claude.voiceaccess.R;

import java.util.ArrayList;
import java.util.List;

public class CommandsActivity extends AppCompatActivity {

    private static final int TYPE_HEADER  = 0;
    private static final int TYPE_SECTION = 1;
    private static final int TYPE_COMMAND = 2;

    private View categoryScroll;
    private RecyclerView commandsRecycler;
    private TextView toolbarTitle;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commands);

        categoryScroll   = findViewById(R.id.category_scroll);
        commandsRecycler = findViewById(R.id.commands_recycler);
        toolbarTitle     = findViewById(R.id.toolbar_title);
        btnBack          = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> {
            if (commandsRecycler.getVisibility() == View.VISIBLE) {
                showCategorySelector();
            } else {
                finish();
            }
        });

        // Category cards
        findViewById(R.id.cat_basics).setOnClickListener(v -> showCategory("basics", "Basics"));
        findViewById(R.id.cat_gestures).setOnClickListener(v -> showCategory("gestures", "Gestures"));
        findViewById(R.id.cat_grid).setOnClickListener(v -> showCategory("grid", "Grid"));
        findViewById(R.id.cat_text).setOnClickListener(v -> showCategory("text", "Text editing"));
        findViewById(R.id.cat_magnification).setOnClickListener(v -> showCategory("magnification", "Magnification"));

        // Check if launched with a category intent
        String category = getIntent().getStringExtra("category");
        if (category != null) {
            showCategory(category, categoryLabel(category));
        }
    }

    private String categoryLabel(String cat) {
        switch (cat) {
            case "basics": return "Basics";
            case "gestures": return "Gestures";
            case "grid": return "Grid";
            case "text": return "Text editing";
            case "magnification": return "Magnification";
            default: return "Commands";
        }
    }

    private void showCategorySelector() {
        toolbarTitle.setText("Voice commands");
        categoryScroll.setVisibility(View.VISIBLE);
        commandsRecycler.setVisibility(View.GONE);
    }

    private void showCategory(String category, String label) {
        toolbarTitle.setText(label);
        categoryScroll.setVisibility(View.GONE);
        commandsRecycler.setVisibility(View.VISIBLE);
        commandsRecycler.setLayoutManager(new LinearLayoutManager(this));
        commandsRecycler.setAdapter(new CommandsAdapter(buildCommandList(category)));
    }

    // ── Data ────────────────────────────────────────────────────────────────

    static class CommandItem {
        final int type;
        final String text;
        CommandItem(int type, String text) { this.type = type; this.text = text; }
    }

    private List<CommandItem> buildCommandList(String category) {
        List<CommandItem> items = new ArrayList<>();
        switch (category == null ? "" : category) {
            case "basics":   buildBasics(items);        break;
            case "gestures": buildGestures(items);      break;
            case "grid":     buildGrid(items);          break;
            case "text":     buildText(items);          break;
            case "magnification": buildMagnification(items); break;
            default:
                buildBasics(items);
                buildGestures(items);
                buildGrid(items);
                buildText(items);
                buildMagnification(items);
                break;
        }
        return items;
    }

    private void h(List<CommandItem> l, String t) { l.add(new CommandItem(TYPE_HEADER,  t)); }
    private void s(List<CommandItem> l, String t) { l.add(new CommandItem(TYPE_SECTION, t)); }
    private void c(List<CommandItem> l, String t) { l.add(new CommandItem(TYPE_COMMAND, t)); }

    private void buildBasics(List<CommandItem> l) {
        h(l, "Basics");
        s(l, "Voice Access");
        c(l, "Stop listening");
        c(l, "Hey Claude");
        c(l, "All commands");
        c(l, "Open tutorial");
        c(l, "Voice Access settings");
        c(l, "Send feedback");
        c(l, "Help");
        c(l, "Cancel");
        c(l, "Again");

        s(l, "General");
        c(l, "Open [app name]");
        c(l, "Go back");
        c(l, "Go home");
        c(l, "Show notifications");
        c(l, "Show quick settings");
        c(l, "Recent apps");
        c(l, "Previous app");
        c(l, "Lock screen");
        c(l, "Take screenshot");
        c(l, "Hide keyboard");
        c(l, "Show keyboard");
        c(l, "Answer call");
        c(l, "Play / Pause");
        c(l, "Volume up / Volume down");
        c(l, "Mute / Unmute");
        c(l, "Ring volume up / down");
        c(l, "Bluetooth on / Bluetooth off");

        s(l, "Numbers & Labels");
        c(l, "Show numbers");
        c(l, "Hide numbers");
        c(l, "Show labels");
        c(l, "Hide labels");
        c(l, "[number]");
        c(l, "What is [number]?");
        c(l, "Show commands for [number]");

        s(l, "Settings");
        c(l, "Search for [query]");
        c(l, "Call [phone number]");
        c(l, "Copy phone number");
    }

    private void buildGestures(List<CommandItem> l) {
        h(l, "Gestures");
        s(l, "Touch gestures");
        c(l, "Tap [element]");
        c(l, "Long press [element]");
        c(l, "Pinch in [element]");
        c(l, "Pinch out [element]");

        s(l, "Swipe gestures");
        c(l, "Scroll up");
        c(l, "Scroll down");
        c(l, "Scroll left");
        c(l, "Scroll right");
        c(l, "Scroll [element] to top");
        c(l, "Scroll [element] to bottom");
        c(l, "Swipe up");
        c(l, "Swipe down");
        c(l, "Swipe left");
        c(l, "Swipe right");
        c(l, "Swipe left edge");
        c(l, "Swipe right edge");
    }

    private void buildGrid(List<CommandItem> l) {
        h(l, "Grid");
        s(l, "General");
        c(l, "Show grid");
        c(l, "Hide grid");
        c(l, "More squares");
        c(l, "Fewer squares");

        s(l, "Touch");
        c(l, "Tap [number]");
        c(l, "Long press [number]");
        c(l, "Pinch in [number]");
        c(l, "Pinch out [number]");
        c(l, "Increment [number]");
        c(l, "Decrement [number]");
        c(l, "Drag [number]");

        s(l, "Swipe");
        c(l, "Swipe [number] up");
        c(l, "Swipe [number] down");
        c(l, "Swipe [number] left");
        c(l, "Swipe [number] right");
    }

    private void buildText(List<CommandItem> l) {
        h(l, "Text editing");
        s(l, "Start / Stop");
        c(l, "Start editing");
        c(l, "Stop editing");

        s(l, "Type");
        c(l, "Type [text]");
        c(l, "Undo");
        c(l, "Redo");
        c(l, "Format email");

        s(l, "Punctuation");
        c(l, "Comma");
        c(l, "Period");
        c(l, "Question mark");
        c(l, "Exclamation point");
        c(l, "Colon");
        c(l, "Semicolon");
        c(l, "Apostrophe");
        c(l, "Open parenthesis / Close parenthesis");
        c(l, "New line");
        c(l, "Smiley face");

        s(l, "Replace");
        c(l, "Replace [old] with [new]");
        c(l, "Replace all [old] with [new]");
        c(l, "Replace everything between [A] and [B]");

        s(l, "Delete");
        c(l, "Delete all");
        c(l, "Delete word");
        c(l, "Delete [word]");
        c(l, "Delete to beginning");
        c(l, "Delete to end");
        c(l, "Delete selected");
        c(l, "Delete from [A] to [B]");
        c(l, "Delete previous [N] words");
        c(l, "Delete next [N] words");

        s(l, "Move cursor");
        c(l, "Go to beginning");
        c(l, "Go to end");
        c(l, "Move after [word]");
        c(l, "Move before [word]");
        c(l, "Move between [A] and [B]");
        c(l, "Move right [N] words");
        c(l, "Move left [N] words");

        s(l, "Select");
        c(l, "Select all");
        c(l, "Unselect");
        c(l, "Select [word]");
        c(l, "Select to beginning");
        c(l, "Select to end");
        c(l, "Select from [A] to [B]");
        c(l, "Select previous [N] words");
        c(l, "Select next [N] words");

        s(l, "Case");
        c(l, "Capitalize [word]");
        c(l, "Uppercase [word]");
        c(l, "Lowercase [word]");

        s(l, "Insert");
        c(l, "Insert before [word]");
        c(l, "Insert after [word]");
        c(l, "Insert between [A] and [B]");
    }

    private void buildMagnification(List<CommandItem> l) {
        h(l, "Magnification");
        s(l, "Zoom");
        c(l, "Start zooming");
        c(l, "Stop zooming");
        c(l, "Zoom in");
        c(l, "Zoom out");

        s(l, "Pan");
        c(l, "Pan left");
        c(l, "Pan right");
        c(l, "Pan up");
        c(l, "Pan down");
    }

    // ── Adapter ─────────────────────────────────────────────────────────────

    static class CommandsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<CommandItem> items;
        CommandsAdapter(List<CommandItem> items) { this.items = items; }

        @Override public int getItemViewType(int pos) { return items.get(pos).type; }
        @Override public int getItemCount()           { return items.size(); }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
            int layout = viewType == TYPE_HEADER  ? R.layout.item_command_header  :
                         viewType == TYPE_SECTION ? R.layout.item_command_section :
                                                    R.layout.item_command;
            View v = android.view.LayoutInflater.from(p.getContext()).inflate(layout, p, false);
            return new GenericVH(v, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder vh, int pos) {
            GenericVH h = (GenericVH) vh;
            CommandItem item = items.get(pos);
            if (h.tv != null) h.tv.setText(item.text);
        }

        static class GenericVH extends RecyclerView.ViewHolder {
            TextView tv;
            GenericVH(View v, int type) {
                super(v);
                if (type == TYPE_HEADER)       tv = v.findViewById(R.id.header_text);
                else if (type == TYPE_SECTION)  tv = v.findViewById(R.id.section_text);
                else                            tv = v.findViewById(R.id.command_text);
            }
        }
    }
}
