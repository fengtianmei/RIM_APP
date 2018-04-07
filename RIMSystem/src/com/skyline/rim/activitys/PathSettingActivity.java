package com.skyline.rim.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.skyline.rim.utils.FileManager;
import com.skyline.rim.utils.localStorageTool;
import com.skyline.terraexplorer.R;

import java.util.ArrayList;
import java.util.List;

import static com.skyline.rim.globaldata.ConstData.GEODATA_PATH_DEFAULT;
import static com.skyline.rim.globaldata.ConstData.GEODATA_PATH_MEMORY;
import static com.skyline.rim.globaldata.ConstData.GEODATA_PATH_SDCARD;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;
import static com.skyline.rim.utils.FileManager.isExist;
import static com.skyline.rim.utils.localStorageTool.getLocalStorageVar;


/**
 * Created by mft on 2017/12/8.
 */

public class PathSettingActivity extends Activity {
    //    private RadioGroup radioGroup;
//    private RadioButton webstorageButton;
//    private RadioButton memorystorageButton;
//    private RadioButton sdstorageButton;
    private List<PathSettingActivity.dataStorePath> permissions;//要显示的数据集合
    private ListView lvPermissions;//ListView对象
    private BaseAdapter permissionAdapt;//适配器

    //图片资源集合
    int[] resImags = {
            R.drawable.defaultstorage, R.drawable.memory, R.drawable.sdcard
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //加载布局文件
        setContentView(R.layout.activity_path_setting);
        init();//初始化要显示的数据集合、ListView对象、以及适配器
    }

    /***
     * 适配器刷新界面显示结果
     * @param permission
     */
    private void setPermissionAvalibleIcon(String permission) {
        int i = 0;
        for (PathSettingActivity.dataStorePath p : permissions) {
            if (p.getPermission().equals(permission)) {
                p.setImage(resImags[0]);
                permissionAdapt.notifyDataSetChanged();
                i++;
            }
        }
    }

    /***
     * 初始化权限数据
     */
    private void init() {
        // TODO Auto-generated method stub
        //初始化要显示的数据集合---start
        permissions = new ArrayList<PathSettingActivity.dataStorePath>();

        //将资源中的字符串组数转换为Java数组
        PathSettingActivity.dataStorePath general = new PathSettingActivity.dataStorePath(resImags[0], getString(R.string.setting_data_path_web), "");

        permissions.add(general);
        general = new PathSettingActivity.dataStorePath(resImags[1], getString(R.string.setting_data_path_memory), "");

        permissions.add(general);
        general = new PathSettingActivity.dataStorePath(resImags[2], getString(R.string.setting_data_path_sd), "");

        permissions.add(general);

        //初始化要显示的数据集合---end
        //初始化listView
        lvPermissions = (ListView) findViewById(R.id.datastorelist);
        //初始化适配器以及设置该listView的适配器
        permissionAdapt = new PathSettingActivity.dataStorePathAdapter();
        lvPermissions.setAdapter(permissionAdapt);
    }

    class dataStorePath {
        public int getImage() {
            return image;
        }

        public void setImage(int image) {
            this.image = image;
        }

        private int image;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private String name;

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        private String permission;

        dataStorePath(int image, String name, String permission) {
            this.image = image;
            this.name = name;
            this.permission = permission;
        }
    }

    class dataStorePathAdapter extends BaseAdapter {

        //得到listView中item的总数
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return permissions.size();
        }

        @Override
        public PathSettingActivity.dataStorePath getItem(int position) {
            // TODO Auto-generated method stub
            return permissions.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        //简单来说就是拿到单行的一个布局，然后根据不同的数值，填充主要的listView的每一个item
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            //拿到ListViewItem的布局，转换为View类型的对象
            View layout = View.inflate(PathSettingActivity.this, R.layout.list_item_datapath, null);
            //找到显示的ImageView
            ImageView ivThumb = (ImageView) layout.findViewById(R.id.ivThumb);
            //找到显示名字的TextView
            TextView tvName = (TextView) layout.findViewById(R.id.tvName);
            //找到显示状态的TextView
            TextView tvStatus = (TextView) layout.findViewById(R.id.tvStatus);
            //获取下标是position的对象
            PathSettingActivity.dataStorePath general = permissions.get(position);
            //设置显示的图像
            ivThumb.setImageResource(general.getImage());
            //显示名字
            tvName.setText(general.getName().replace("skylinedata", "skylinedata/" + mProjectID));
            String path = "";
            switch (position) {
                case 1:
                    path = FileManager.getMemoryDataPath(PathSettingActivity.this);
                    if (!isExist(path))
                        tvStatus.setText("未启用");
                    else tvStatus.setText("已启用");
                    break;
                case 2:
                    path = FileManager.getSDDataPath(PathSettingActivity.this);
                    if (!isExist(path))
                        tvStatus.setText("未启用");
                    else tvStatus.setText("已启用");
                    break;
                default:
                    tvStatus.setText("已启用");
                    break;
            }
            return layout;
        }
    }
}
