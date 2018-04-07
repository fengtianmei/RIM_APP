package com.skyline.terraexplorer.models;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;

import java.util.ArrayList;
import java.util.Arrays;

public class TableDataSource {

    private class ListAdapter extends BaseExpandableListAdapter {
        private final class ViewHolder {
            TextView text;
            TextView detailText;
            ImageView icon;
            ImageView accessoryButton;
            TextView accessoryText;
            View progressIcon;
        }

        protected final LayoutInflater inflater;
        private OnClickListener tableRowClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (swipeDetector.isSwipeDetected() == false)
                    delegate.didSelectRowAtIndexPath(tableView.getItemIdAtPosition(tableView.getPositionForView(v)));
            }
        };
        private OnClickListener accessoryViewClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (swipeDetector.isSwipeDetected() == false)
                    delegate.accessoryButtonTappedForRowWithIndexPath(tableView.getItemIdAtPosition(tableView.getPositionForView(v)));
            }
        };

        private OnLongClickListener tableRowLongClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (swipeDetector.isSwipeDetected() == false)
                    showContextMenu(v);
                return false;
            }
        };

        public ListAdapter() {
            inflater = LayoutInflater.from(TEApp.getAppContext());
        }

        @Override
        public int getGroupCount() {
            return TableDataSource.this.dataItems.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (TableDataSource.this.dataItems.size() > groupPosition)
                return TableDataSource.this.dataItems.get(groupPosition).childItems.size();
            return 0;
        }

        @Override
        public DisplayGroupItem getGroup(int groupPosition) {
            return TableDataSource.this.dataItems.get(groupPosition);
        }

        @Override
        public DisplayItem getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).childItems.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return ExpandableListView.getPackedPositionForChild(groupPosition, childPosition);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getGroupTypeCount() {
            return 2;
        }

        @Override
        public int getGroupType(int groupPosition) {
            return getGroup(groupPosition).name == null ? 1 : 0;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            ViewHolder holder;

            DisplayGroupItem group = getGroup(groupPosition);
            if (convertView == null) {
                if (getGroupType(groupPosition) == 0) {
                    convertView = inflater.inflate(R.layout.expandable_list_header, parent, false);
                    holder = new ViewHolder();
                    holder.text = (TextView) convertView;
                    convertView.setTag(holder);
                } else {
                    // just setting view height to 0 does not work. it is changed by listview to wrap content
                    // However in onAttachedToWindow it does work
                    convertView = new View(parent.getContext()) {
                        protected void onAttachedToWindow() {
                            getLayoutParams().height = 0;
                        }

                        ;

                    };
                    convertView.setTag(new ViewHolder());
                }
            }

            holder = (ViewHolder) convertView.getTag();

            if (group.name != null) {
                holder.text.setText(group.name);
            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            final DisplayItem item = getChild(groupPosition, childPosition);
            final long packedPosition = getCombinedChildId(groupPosition, childPosition);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.expandable_list_item, parent, false);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.table_cell_text);
                holder.detailText = (TextView) convertView.findViewById(R.id.table_cell_detail_text);
                holder.icon = (ImageView) convertView.findViewById(R.id.table_cell_icon);
                holder.accessoryButton = (ImageView) convertView.findViewById(R.id.table_cell_accessoryIcon);
                holder.accessoryText = (TextView) convertView.findViewById(R.id.table_cell_accessoryText);
                holder.progressIcon = convertView.findViewById(R.id.table_cell_accessoryProgress);
                convertView.setTag(holder);
                convertView.setClickable(true);
                convertView.setOnClickListener(tableRowClickListener);
                convertView.setOnLongClickListener(tableRowLongClickListener);
                convertView.setOnTouchListener(swipeDetector);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.text.setVisibility(View.VISIBLE);
            holder.detailText.setVisibility(View.VISIBLE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.accessoryButton.setVisibility(View.VISIBLE);
            holder.accessoryText.setVisibility(View.VISIBLE);
            holder.progressIcon.setVisibility(View.VISIBLE);

            if (TextUtils.isEmpty(item.attributedName) == false)
                holder.text.setText(item.attributedName);
            else
                holder.text.setText(item.name);

            if (TextUtils.isEmpty(item.subTitle) == false)
                holder.detailText.setText(item.subTitle);
            else
                holder.detailText.setVisibility(View.GONE);

            if (item.icon != 0)
                holder.icon.setImageResource(item.icon);
            else
                holder.icon.setVisibility(View.GONE);

            if (item.accessoryIcon != 0) {
                if (item.accessoryIcon == DisplayItem.ProgressIcon) {
                    holder.accessoryButton.setVisibility(View.GONE);
                } else {
                    holder.progressIcon.setVisibility(View.GONE);
                    holder.accessoryButton.setImageResource(item.accessoryIcon);
                }
            } else {
                holder.progressIcon.setVisibility(View.GONE);
                holder.accessoryButton.setVisibility(View.GONE);
            }

            if (delegate.accessoryButtonTappableForRowWithIndexPath(packedPosition)) {
                holder.accessoryButton.setColorFilter(Color.GRAY);
                holder.accessoryButton.setOnTouchListener(swipeDetector);
                holder.accessoryButton.setOnClickListener(accessoryViewClickListener);

            } else {
                holder.accessoryButton.clearColorFilter();
                holder.accessoryButton.setOnTouchListener(null);
                holder.accessoryButton.setClickable(false);
            }

            if (TextUtils.isEmpty(item.accessoryText))
                holder.accessoryText.setVisibility(View.GONE);
            else
                holder.accessoryText.setText(item.accessoryText);

            if (item.disabled) {
                convertView.setAlpha(0.3f);
                convertView.setEnabled(false);
            } else {
                convertView.setAlpha(1);
                convertView.setEnabled(true);
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

    // adopted from http://stackoverflow.com/questions/4373485/android-swipe-on-list/9340202#9340202
    private class SwipeDetector implements View.OnTouchListener {
        private boolean swipeDetected = false;
        private MotionEvent downEvent;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (contextMenu != null) {
                        hideContextMenu();
                        return true;
                    }
                    downEvent = MotionEvent.obtain(event);
                    swipeDetected = false;
                    return false; // allow other events like Click to be processed
                case MotionEvent.ACTION_UP:
                    float deltaX = downEvent.getX() - event.getX();
                    float deltaY = downEvent.getY() - event.getY();


                    // horizontal right to left swipe detection
                    if (deltaX > 30 /* swipe distance */ && Math.abs(deltaY) < 60) {
                        swipeDetected = true;
                        showContextMenu(v);
                        return true;
                    }
                    return false;
            }
            return false;
        }

        public boolean isSwipeDetected() {
            return swipeDetected;
        }
    }

    private ArrayList<DisplayGroupItem> dataItems;
    private TableDataSourceDelegate delegate;
    private ExpandableListView tableView;
    private ListAdapter listAdapter;
    private LinearLayout contextMenu;
    private SwipeDetector swipeDetector = new SwipeDetector();

    private OnClickListener contextMenuOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            contextMenuButtonClick(v);
        }
    };

    private static final int AccessoryButtonInContextMenu = 454709654;

    public interface TableDataSourceDelegate {
        ContextMenuEntry[] contextMenuForPath(long packedPosition);

        void contextMenuItemTapped(int menuId, long packedPosition);

        void accessoryButtonTappedForRowWithIndexPath(long packedPosition);

        boolean accessoryButtonTappableForRowWithIndexPath(long packedPosition);

        void didSelectRowAtIndexPath(long packedPosition);
    }

    public TableDataSource(ExpandableListView tableView, TableDataSourceDelegate delegate) {
        this.delegate = delegate;
        this.listAdapter = new ListAdapter();
        this.dataItems = new ArrayList<DisplayGroupItem>();
        setTableView(tableView);
    }

    private void setTableView(ExpandableListView tableView) {
        this.tableView = tableView;
        this.tableView.setAdapter(this.listAdapter);
        this.tableView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                return true; // This way the expander cannot be collapsed
            }
        });
    }

    public void setDataItems(DisplayGroupItem[] dataItems) {
        setDataItems(new ArrayList<DisplayGroupItem>(Arrays.asList(dataItems)));
    }

    public void setDataItems(ArrayList<DisplayGroupItem> dataItems) {
        this.hideContextMenu();
        ArrayList<DisplayGroupItem> filteredDataItems = new ArrayList<DisplayGroupItem>();
        for (DisplayGroupItem item : dataItems) {
            if (item.childItems.size() > 0)
                filteredDataItems.add(item);
        }
        this.dataItems = filteredDataItems;
        this.reloadData();
        for (int i = 0; i < dataItems.size(); i++) {
            this.tableView.expandGroup(i);
        }
    }

    public DisplayItem getItemForPath(long packedPosition) {
        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
        int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
        if (groupPosition < 0 || childPosition < 0)
            return null;
        return listAdapter.getChild(groupPosition, childPosition);
    }

    private void hideContextMenu() {
        if (contextMenu == null)
            return;

        int position = (Integer) contextMenu.getTag();
        long packedPosition = tableView.getItemIdAtPosition(position);
        int padding = 0;
        final DisplayItem item = getItemForPath(packedPosition);
        final View cell = getCellForPosition(position);

        if (item.accessoryIcon != 0)
            padding = (int) tableView.getContext().getResources().getDimension(R.dimen.table_row_height);
        ObjectAnimator animOffsetX = ObjectAnimator.ofFloat(contextMenu, View.TRANSLATION_X, contextMenu.getWidth() - padding);
        ObjectAnimator animColor = ObjectAnimator.ofFloat(contextMenu, View.ALPHA, 0.5f);
        AnimatorSet set = new AnimatorSet().setDuration(200);
        final LinearLayout _contextMenu = contextMenu;
        contextMenu = null;
        set.playTogether(animOffsetX, animColor);
        set.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cell.findViewById(R.id.table_cell_accessoryView).setVisibility(View.VISIBLE);
                if (contextMenu == _contextMenu) {
                    _contextMenu.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        set.start();

    }

    private void showContextMenu(View view) {
        int position = tableView.getPositionForView(view);
        long packedPosition = tableView.getItemIdAtPosition(position);
        DisplayItem displayItem = getItemForPath(packedPosition);
        if (displayItem == null || contextMenu != null)
            return;

        ContextMenuEntry[] menuEntriesArray = delegate.contextMenuForPath(packedPosition);
        if (menuEntriesArray == null || menuEntriesArray.length == 0)
            return;
        ArrayList<ContextMenuEntry> menuEntries = new ArrayList<ContextMenuEntry>((Arrays.asList(menuEntriesArray)));
        int buttonSize = (int) tableView.getContext().getResources().getDimension(R.dimen.table_row_height);
        int padding = 0;
        if (displayItem.accessoryIcon != 0) {
            menuEntries.add(0, new ContextMenuEntry(displayItem.accessoryIcon, AccessoryButtonInContextMenu));
            padding = buttonSize;
        }

        View cell = getCellForPosition(position);
        contextMenu = (LinearLayout) cell.findViewById(R.id.table_cell_contextMenu);
        contextMenu.setTag(position);
        contextMenu.removeAllViews();
        int imagePadding = (int) tableView.getContext().getResources().getDimension(R.dimen.button_padding);
        for (ContextMenuEntry contextMenuEntry : menuEntries) {
            ImageView button = new ImageView(tableView.getContext());
            button.setImageResource(contextMenuEntry.icon);
            button.setScaleType(ScaleType.FIT_CENTER);
            button.setPadding(imagePadding, imagePadding, imagePadding, imagePadding);
            button.setTag(contextMenuEntry.id);
            button.setOnClickListener(contextMenuOnClickListener);
            // if there are children, add separator
            if (contextMenu.getChildCount() > 0) {
                View separator = new View(tableView.getContext());
                separator.setBackgroundColor(Color.GRAY);
                contextMenu.addView(separator, 2, buttonSize);
            }

            contextMenu.addView(button, buttonSize, buttonSize);
        }

        cell.findViewById(R.id.table_cell_accessoryView).setVisibility(View.GONE);
        contextMenu.setVisibility(View.VISIBLE);
        contextMenu.setTranslationX(menuEntries.size() * buttonSize - padding);
        ObjectAnimator
                .ofFloat(contextMenu, View.TRANSLATION_X, 0)
                .setDuration(200)
                .start();
    }

    private View getCellForPosition(int position) {
        int firstPosition = tableView.getFirstVisiblePosition() - tableView.getHeaderViewsCount(); // This is the same as child #0
        int wantedChild = position - firstPosition;
        if (wantedChild < 0 || wantedChild >= tableView.getChildCount()) {
            return null;
        }
        return tableView.getChildAt(wantedChild);
    }

    private void contextMenuButtonClick(View v) {
        hideContextMenu();
        int position = tableView.getPositionForView(v);
        long packedPosition = tableView.getItemIdAtPosition(position);
        int menuId = (Integer) v.getTag();
        if (menuId == AccessoryButtonInContextMenu)
            delegate.accessoryButtonTappedForRowWithIndexPath(packedPosition);
        else
            delegate.contextMenuItemTapped(menuId, packedPosition);
    }

    public void reloadData() {
        this.listAdapter.notifyDataSetChanged();
    }
}
