package com.skyline.terraexplorer.tools;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.widget.Toast;

import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.UI;

import java.io.File;
import java.util.concurrent.Callable;

public class CaptureShareTool extends BaseTool {
    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_more_share, R.drawable.share, 60);
    }

    @Override
    public void open(Object param) {
        String path = UI.runOnRenderThread(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return ISGWorld.getInstance().getWindow().GetSnapShot(true, 0, 0, "JPeg75");
            }
        });
        Uri uri = Uri.fromFile(new File(path));

        MediaScannerConnection.scanFile(TEApp.getAppContext(), new String[]{path}, new String[]{"image/jpeg"}, null);
        Toast.makeText(TEApp.getAppContext(), TEApp.getAppContext().getString(R.string.share_snapshot_taken), Toast.LENGTH_SHORT).show();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        TEApp.getCurrentActivityContext().startActivity(Intent.createChooser(share, TEApp.getAppContext().getString(R.string.share_snapshot_share)));
    }

}
