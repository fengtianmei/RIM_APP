package com.skyline.terraexplorer.controllers;

import android.os.Bundle;
import android.text.InputType;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.ContextMenuEntry;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.tools.WhiteboardTool;
import com.skyline.terraexplorer.tools.WhiteboardTool.Whiteboard;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialog.ModalDialogDelegate;

public class WhiteboardListActivity extends MatchParentActivity implements ModalDialogDelegate {
    private TableDataSource dataSource;

    public static WhiteboardTool delegate;
    private TableDataSourceDelegateBase dataSourceDelegate = new TableDataSourceDelegateBase() {
        public void didSelectRowAtIndexPath(long packedPosition) {
            WhiteboardListActivity.this.didSelectRowAtIndexPath(packedPosition);
        }

        public ContextMenuEntry[] contextMenuForPath(long packedPosition) {
            return WhiteboardListActivity.this.contexMenuForPath(packedPosition);
        }

        ;

        public void contextMenuItemTapped(int menuId, long packedPosition) {
            WhiteboardListActivity.this.contextMenuItemTapped(menuId, packedPosition);
        }

        ;
    };

    private int modalDialogMode;
    private static final int MD_DELETE = 1;
    private static final int MD_RENAME = 2;
    private static final int MD_ADD = 3;
    private static final int MD_EDIT = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UI.addHeader(R.string.title_activity_whiteboards, R.drawable.list, this);
        dataSource = new TableDataSource(UI.addFullScreenTable(this), dataSourceDelegate);
        bindData();
    }

    private void bindData() {
        DisplayGroupItem boardList = new DisplayGroupItem(null);
        for (int i = 0; i < delegate.boards.size(); i++) {
            Whiteboard board = delegate.boards.get(i);
            DisplayItem item = new DisplayItem(board.name);
            item.tag = i;
            if (board.visible)
                item.icon = R.drawable.checkbox_on;
            else
                item.icon = R.drawable.checkbox_off;
            if (board == delegate.getCurrentBoard()) {
                item.accessoryText = getString(R.string.whiteboard_current);
            }
            boardList.childItems.add(item);
        }
        DisplayItem item = new DisplayItem(R.string.whiteboard_new_row);
        item.tag = -1;
        boardList.childItems.add(item);
        dataSource.setDataItems(new DisplayGroupItem[]{boardList});
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        if (item.tag == -1) {
            ModalDialog dlg = new ModalDialog(R.string.whiteboard_new, this);
            dlg.setContentTextField(InputType.TYPE_CLASS_TEXT, getString(R.string.whiteboard_default_name));
            modalDialogMode = MD_ADD;
            dlg.show();
        } else {
            Whiteboard board = delegate.boards.get(item.tag);
            board.visible = !board.visible;
            bindData();
            delegate.updateFeaturesVisibility();
            delegate.saveBoardList();
        }

    }

    protected ContextMenuEntry[] contexMenuForPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        if (item.tag == -1)
            return null;
        if (delegate.boards.size() > 1)
            return new ContextMenuEntry[]{
                    new ContextMenuEntry(R.drawable.properties, MD_EDIT),
                    new ContextMenuEntry(R.drawable.rename, MD_RENAME),
                    new ContextMenuEntry(R.drawable.delete, MD_DELETE),
            };
        else
            return new ContextMenuEntry[]{
                    new ContextMenuEntry(R.drawable.properties, MD_EDIT),
                    new ContextMenuEntry(R.drawable.rename, MD_RENAME),
            };
    }

    protected void contextMenuItemTapped(int menuId, long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        ModalDialog dlg = null;
        switch (menuId) {
            case MD_DELETE: // delete
            {
                dlg = new ModalDialog(R.string.delete, this);
                dlg.setContentMessage(String.format(getString(R.string.Whiteboard_delete), item.name));
                break;
            }
            case MD_RENAME: // rename
            {
                dlg = new ModalDialog(R.string.rename, this);
                dlg.setContentTextField(InputType.TYPE_CLASS_TEXT, item.name);
                break;
            }
            case MD_EDIT: // edit
            {
                delegate.setCurrentBoard(delegate.boards.get(item.tag));
                bindData();
                break;
            }
            default:
                break;
        }
        if (dlg != null) {
            modalDialogMode = menuId;
            dlg.setTag(item);
            dlg.show();
        }

    }

    @Override
    public void modalDialogDidDismissWithOk(ModalDialog dlg) {
        DisplayItem item = (DisplayItem) dlg.getTag();
        switch (modalDialogMode) {
            case MD_DELETE: {
                delegate.deleteBoard(delegate.boards.get(item.tag));
                break;
            }
            case MD_RENAME: {
                Whiteboard board = delegate.boards.get(item.tag);
                board.name = dlg.getTextField().getText().toString();
                delegate.setCurrentBoard(board);
                break;
            }
            case MD_ADD: {
                Whiteboard board = new Whiteboard(dlg.getTextField().getText().toString());
                delegate.boards.add(board);
                delegate.updateFeaturesVisibility();
                delegate.setCurrentBoard(board);
                break;
            }
        }
        delegate.saveBoardList();
        bindData();
    }

    @Override
    public void modalDialogDidDismissWithCancel(ModalDialog dlg) {
        // not in use
    }


}
