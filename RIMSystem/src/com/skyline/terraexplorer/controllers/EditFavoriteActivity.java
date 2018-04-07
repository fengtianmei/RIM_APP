package com.skyline.terraexplorer.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.FavoriteItem;
import com.skyline.terraexplorer.models.FavoritesStorage;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;

public class EditFavoriteActivity extends MatchParentActivity {

    private class ModalDialogDelegate extends ModalDialogDelegateBase {
        @Override
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            EditFavoriteActivity.this.modalDialogDidDismissWithOk(dlg);
        }
    }

    private TextView nameValue;
    private View nameContrainer;
    private TextView descriptionValue;
    private View descriptionContrainer;
    private TextView iconValue;
    private View iconContrainer;
    private ImageView iconImage;
    private Switch showOn3D;
    private View showOn3DContainer;
    private FavoriteItem currentItem;
    private ModalDialogDelegate modalDialogDelegate = new ModalDialogDelegate();
    private static final int EDIT_NAME = 1;
    private static final int EDIT_DESCRIPTION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_favorite);
        nameValue = (TextView) findViewById(R.id.favorite_name);
        nameContrainer = (View) nameValue.getParent();
        descriptionValue = (TextView) findViewById(R.id.favorite_description);
        descriptionContrainer = (View) descriptionValue.getParent();
        iconValue = (TextView) findViewById(R.id.favorite_icon_name);
        iconImage = (ImageView) findViewById(R.id.favorite_icon_image);
        iconContrainer = (View) iconValue.getParent();
        showOn3D = (Switch) findViewById(R.id.favorite_show_on_3d);
        showOn3DContainer = (View) showOn3D.getParent();
        String favoriteItemId = getIntent().getExtras().getString(FavoriteItem.FAVORITE_ID);
        int header;
        if (favoriteItemId == null) {
            header = R.string.title_activity_favorites;
            currentItem = new FavoriteItem();
            currentItem.name = getString(R.string.favorites_default_name);
        } else {
            header = R.string.title_activity_favorites;
            currentItem = FavoritesStorage.defaultStorage.getItem(favoriteItemId);
            if (currentItem == null) {
                Toast toast = Toast.makeText(this, getString(R.string.favorites_item_not_found), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                finish();
                return;
            }
        }

        nameContrainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                nameTapped(v);
            }
        });

        iconContrainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                iconTaped(v);
            }
        });

        descriptionContrainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                descriptionTaped(v);
            }
        });

        showOn3D.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showOn3DValueChanged(buttonView);
            }
        });
        showOn3DContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showOn3D.setChecked(!showOn3D.isChecked());
                showOn3DValueChanged(showOn3D);
            }
        });

        nameValue.setText(currentItem.name);
        showOn3D.setChecked(currentItem.showOn3D);
        descriptionValue.setText(currentItem.desc);
        setIconNewValue(currentItem.icon);
        showOn3DValueChanged(showOn3D);

        // add header
        UI.addHeader(header, R.drawable.favorits, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FavoritesStorage.defaultStorage.saveItem(currentItem);
    }

    private void nameTapped(View sender) {
        showTextEditor(R.string.favorite_name, (String) nameValue.getText(), EDIT_NAME);
    }

    private void descriptionTaped(View sender) {
        showTextEditor(R.string.favorite_description, (String) descriptionValue.getText(), EDIT_DESCRIPTION);
    }

    private void setNameNewValue(String newValue) {
        nameValue.setText(newValue);
        currentItem.name = newValue;
    }

    private void setDescriptionNewValue(String newValue) {
        descriptionValue.setText(newValue);
        currentItem.desc = newValue;
    }

    private void showTextEditor(int title, String value, int editMode) {

        ModalDialog modalDialog = new ModalDialog(title, modalDialogDelegate);
        modalDialog.setContentTextField(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, value);
        modalDialog.setTag(editMode);
        modalDialog.show();
    }

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        String newValue = dlg.getTextField().getText().toString();
        switch ((Integer) dlg.getTag()) {
            case EDIT_NAME: {
                setNameNewValue(newValue);
                break;
            }
            case EDIT_DESCRIPTION: {
                setDescriptionNewValue(newValue);
                break;
            }
        }
    }

    private void iconTaped(View sender) {
        Intent intent = new Intent(this, EditFavoriteSelectIconActivity.class);
        intent.putExtra(FavoriteItem.FAVORITE_ICON, currentItem.icon);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            setIconNewValue(data.getExtras().getInt(FavoriteItem.FAVORITE_ICON));
        }
    }

    private void setIconNewValue(int iconId) {
        currentItem.icon = iconId;
        iconValue.setText(FavoritesStorage.defaultStorage.descriptionForIcon(currentItem.icon));
        iconImage.setImageResource(FavoritesStorage.defaultStorage.resourceForIcon(currentItem.icon));
    }

    @SuppressWarnings("deprecation")
    private void showOn3DValueChanged(View sender) {

        Switch cliker = (Switch) sender;

        if (cliker.isChecked()) {
            iconContrainer.setClickable(true);
            iconContrainer.setAlpha(1);
            descriptionContrainer.setClickable(true);
            descriptionContrainer.setAlpha(1);
            iconImage.setAlpha(255);
        } else {
            iconContrainer.setClickable(false);
            iconContrainer.setAlpha(.3f);
            iconImage.setAlpha((int) (255 * 0.3));
            descriptionContrainer.setClickable(false);
            descriptionContrainer.setAlpha(.3f);
        }
        currentItem.showOn3D = cliker.isChecked();
    }
}
