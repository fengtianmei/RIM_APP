package com.skyline.terraexplorer.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.MenuEntry;

import java.util.ArrayList;
import java.util.HashMap;

public class MainMenuView extends LinearLayout implements OnItemClickListener {

    public interface MainMenuViewDelegate {
        void onMainMenuShow();

        void onMainMenuHide();

        void onMainMenuAction(MenuEntry menuEntry);
    }

    private class MainMenuTableAdapter extends BaseAdapter {
        private MenuEntry menuEntry;
        private LayoutInflater inflater;

        public MainMenuTableAdapter() {
            inflater = LayoutInflater.from(getContext());
            setMenuEntry(new MenuEntry());
        }

        public void setMenuEntry(MenuEntry newMenuEntry) {
            menuEntry = newMenuEntry;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return menuEntry.childEntries().size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MenuEntry entry = menuEntry.childEntries().get(position);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.main_menu_list_item, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.main_menu_text);
                holder.icon = (ImageView) convertView.findViewById(R.id.main_menu_icon);
                holder.bullet = (ImageView) convertView.findViewById(R.id.main_menu_bullet);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.text.setText(entry.text);
            holder.icon.setImageResource(entry.icon);
            int groupIndicator = entry.childEntries().size() == 0 ? View.GONE : View.VISIBLE;
            holder.bullet.setVisibility(groupIndicator);
            if (entry.text == R.string.title_activity_search) {
                LayerDrawable drawable = (LayerDrawable) getContext().getResources().getDrawable(R.drawable.mm_search_background);
                convertView.setBackgroundDrawable(drawable);
                GradientDrawable sd = (GradientDrawable) drawable.getDrawable(0);
                sd.setCornerRadius((getContext().getResources().getDimension(R.dimen.main_menu_row_height) - getContext().getResources().getDimension(R.dimen.main_menu_padding_v) * 2) / 2);
                sd.setStroke(1, Color.GRAY);
                sd.setColor(getContext().getResources().getColor(R.color.color_control_background) & 0xe6ffffff);
            } else {
                convertView.setBackgroundDrawable(null);
            }
            return convertView;
        }

        public class ViewHolder {
            public TextView text;
            public ImageView icon;
            public ImageView bullet;
        }
    }

    private MenuEntry rootMenuEntry;
    private MenuEntry currentMenuEntry;
    private ListView menuTable;
    private TextView menuTitle;
    private ImageButton backButton;
    private View separator;
    private MainMenuTableAdapter menuTableAdapter;
    private MainMenuViewDelegate delegate;

    public MainMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.main_menu, this);
        if (isInEditMode())
            return;
        menuTable = (ListView) findViewById(android.R.id.list);
        backButton = (ImageButton) findViewById(R.id.main_menu_header_back);
        separator = (View) findViewById(R.id.main_menu_header_separator);
        menuTitle = (TextView) findViewById(R.id.main_menu_header_text);

        this.setVisibility(View.GONE);

        menuTableAdapter = new MainMenuTableAdapter();
        menuTable.setAdapter(menuTableAdapter);
        menuTable.setOnItemClickListener(this);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurentMenuEntry(rootMenuEntry);
            }
        });
    }

    private void setCurentMenuEntry(MenuEntry menuEntry) {
        currentMenuEntry = menuEntry;
        menuTitle.setText(currentMenuEntry.text);
        backButton.setVisibility(rootMenuEntry == currentMenuEntry ? View.GONE : View.VISIBLE);
        separator.setVisibility(backButton.getVisibility());

        updateHeight();
        menuTableAdapter.setMenuEntry(menuEntry);
        // the only way I found that worked to reset listview position
        menuTable.setAdapter(menuTableAdapter);
    }

    private int heightForMenuEntriesCount(int count) {
        int maxMenuItems = 6;
        int displayHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        int rowHeight = getResources().getDimensionPixelSize(R.dimen.main_menu_row_height);

        int numberOfMenuItems = Math.min(displayHeight / rowHeight - 3, count);
        numberOfMenuItems = Math.min(numberOfMenuItems, maxMenuItems);
        return rowHeight * numberOfMenuItems;
    }

    private MenuEntry buildMenuFromEntries(ArrayList<MenuEntry> menuEntries) {
        HashMap<String, MenuEntry> entriesByName = new HashMap<String, MenuEntry>();
        MenuEntry rootEntry = new MenuEntry();
        rootEntry.text = R.string.mm_menu;
        String rootEntryText = Integer.toString(rootEntry.text);
        entriesByName.put(rootEntryText, rootEntry);
        for (MenuEntry me : menuEntries) {
            String parent = me.parent;
            if (parent == null)
                parent = rootEntryText;

            MenuEntry parentEntry = entriesByName.get(parent);
            if (parentEntry == null) {
                parentEntry = new MenuEntry();
                String[] menuParts = parent.split("\\|");
                parentEntry.text = Integer.parseInt(menuParts[0]);
                if (menuParts.length > 1)
                    parentEntry.icon = Integer.parseInt(menuParts[1]);
                if (menuParts.length > 2)
                    parentEntry.order = Integer.parseInt(menuParts[2]);
                entriesByName.put(parent, parentEntry);
                rootEntry.addChildEntryOrdered(parentEntry);
            }
            parentEntry.addChildEntryOrdered(me);
        }
        return rootEntry;
    }

    public void setAnchor(View anchor) {
        RelativeLayout.LayoutParams anchorParams = (RelativeLayout.LayoutParams) anchor.getLayoutParams();
        RelativeLayout.LayoutParams myParams = (RelativeLayout.LayoutParams) getLayoutParams();
        //myParams.bottomMargin = anchorParams.bottomMargin + anchorParams.height - (int)UI.dp2px(10 * UI.scaleFactor());
        myParams.topMargin = anchorParams.topMargin + anchorParams.height;// + (int)UI.dp2px(10 * UI.scaleFactor());
        myParams.leftMargin = anchorParams.leftMargin;
        this.setLayoutParams(myParams);
    }

    public void setDelegate(MainMenuViewDelegate delegate) {
        this.delegate = delegate;
    }

    public void show(ArrayList<MenuEntry> menuEntries) {
        delegate.onMainMenuShow();
        setVisibility(View.VISIBLE);
        rootMenuEntry = buildMenuFromEntries(menuEntries);
        setCurentMenuEntry(rootMenuEntry);
        final int targetHeight = heightForMenuEntriesCount(currentMenuEntry.childEntries().size());
        menuTable.getLayoutParams().height = 1;
        setAlpha(0);

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                menuTable.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                setAlpha(1 * interpolatedTime);
                menuTable.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(200);
        menuTable.startAnimation(a);
    }

    public void hide() {
        setVisibility(View.GONE);
        currentMenuEntry = null;
        delegate.onMainMenuHide();
    }

    public void updateHeight() {
        if (currentMenuEntry == null)
            return;
        int height = heightForMenuEntriesCount(currentMenuEntry.childEntries().size());
        ViewGroup.LayoutParams p = menuTable.getLayoutParams();
        p.height = height;
        menuTable.setLayoutParams(p);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        // no idea how it happens, but sometimes it does :(
        if (currentMenuEntry == null)
            return;
        ////////////////////////////////////

        MenuEntry menuEntry = currentMenuEntry.childEntries().get(position);
        if (menuEntry.childEntries().size() > 0) {
            setCurentMenuEntry(menuEntry);
        } else {
            hide();
            delegate.onMainMenuAction(menuEntry);
        }
    }
}
