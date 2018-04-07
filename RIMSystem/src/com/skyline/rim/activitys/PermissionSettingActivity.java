package com.skyline.rim.activitys;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.skyline.rim.utils.PermissionHelper;
import com.skyline.terraexplorer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mft on 2017/10/9.
 */

public class PermissionSettingActivity extends Activity {
    private List<Permission> permissions;//要显示的数据集合
    private ListView lvPermissions;//ListView对象
    private BaseAdapter permissionAdapt;//适配器


    private static final int TSDI_RESULT_CODE = 12;

    //权限检测类
    private PermissionHelper mPermissionHelper;

    //图片资源集合
    int[] resImags = {
            R.drawable.checkbox_green, R.drawable.disablebox_red
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        init();//初始化要显示的数据集合、ListView对象、以及适配器
        mPermissionHelper = new PermissionHelper(PermissionSettingActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    /***
     * 适配器刷新界面显示结果
     * @param permission
     */
    private void setPermissionAvalibleIcon(String permission) {
        int i = 0;
        for (Permission p : permissions) {
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
        permissions = new ArrayList<Permission>();

        //将资源中的字符串组数转换为Java数组
        Permission general = new Permission(resImags[0], getString(R.string.info_permission_camera), Manifest.permission.CAMERA);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_internet), Manifest.permission.INTERNET);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_internet_status), Manifest.permission.ACCESS_NETWORK_STATE);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_gps), Manifest.permission.ACCESS_FINE_LOCATION);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_netlocation), Manifest.permission.ACCESS_COARSE_LOCATION);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_sd_read), Manifest.permission.READ_EXTERNAL_STORAGE);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_sd_write), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_sd_unmount), Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        general = new Permission(resImags[0], getString(R.string.info_permission_sd_format), Manifest.permission.MOUNT_FORMAT_FILESYSTEMS);
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.MOUNT_FORMAT_FILESYSTEMS)) != PackageManager.PERMISSION_GRANTED)
            general.setImage(resImags[1]);
        permissions.add(general);
        //初始化要显示的数据集合---end
        //初始化listView
        lvPermissions = (ListView) findViewById(R.id.permissionlist);
        //初始化适配器以及设置该listView的适配器
        permissionAdapt = new PermissionAdapter();
        lvPermissions.setAdapter(permissionAdapt);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //假设只选择了一个权限检测 grantResults这个集合的长度为1
//        for (int i = 0; i < grantResults.length; i++) {
//            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, getString(R.string.info_permission_start) + permissions[i], Toast.LENGTH_SHORT).show();
//                setPermissionAvalibleIcon(permissions[i]);
//                //用户同意了操作权限
//            } else {
//                //用户拒绝了操作权限
//                //setPermissionAvalible(permissions[i]);
//                Toast.makeText(this, getString(R.string.info_permission_fail) + permissions[i], Toast.LENGTH_SHORT).show();
//            }
//        }

//        switch(requestCode){
//            case TSDI_RESULT_CODE:
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    //如果请求成功，则进行相应的操作，比如我们这里打开一个新的Activity
//                    Toast.makeText(this, getString(R.string.info_permission_start) + permissions[0], Toast.LENGTH_SHORT).show();
//                    setPermissionAvalibleIcon(permissions[0]);
//                } else {
//                    //如果请求失败
//                    //mPermissionHelper.startAppSettings();
//                    Toast.makeText(this, getString(R.string.info_permission_fail) + permissions[0], Toast.LENGTH_SHORT).show();
//                }
//                break;
//            default:
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }


        if (requestCode == TSDI_RESULT_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {//授权成功

                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {//点击拒绝授权

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this
                            , permissions[0])) {//点击拒绝，再次弹出

                        ActivityCompat.requestPermissions(this, new String[]{permissions[0]}, 1);

                    } else { // 选择不再询问，并点击拒绝，弹出提示框
                        //mPermissionHelper.showMissingPermissionDialog();
                        Uri uri = Uri.parse("package:" + getPackageName());//包名
                        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", uri);
                        startActivity(intent);

//                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//                        builder.setMessage("当前应用缺少必要权限。\n\n请点击\"设置\"-\"权限\"-打开所需权限。\n\n最后点击两次后退按钮，即可返回。");
//                        //设置对话框是可取消的
//                        builder.setCancelable(true);
//                        final AlertDialog alertDialog = builder.create();
//                        // 拒绝, 退出应用
//                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                            @Override public void onClick(DialogInterface dialog, int which) {
//                                alertDialog.dismiss();
//                            }
//                        });
//                        builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
//                            @Override public void onClick(DialogInterface dialog, int which) {
//                                Uri uri=Uri.parse("package:"+getPackageName());//包名
//                                Intent intent=new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", uri);
//                                startActivity(intent);
//                            }
//                        });
//
//                        alertDialog.show();
                    }
                }
            }
            Log.e("grantResults", "授权结果" + (grantResults[0] == PackageManager.PERMISSION_GRANTED));
        }


    }

    class Permission {
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

        Permission(int image, String name, String permission) {
            this.image = image;
            this.name = name;
            this.permission = permission;
        }
    }

    class PermissionAdapter extends BaseAdapter {

        //得到listView中item的总数
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return permissions.size();
        }

        @Override
        public Permission getItem(int position) {
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
            View layout = View.inflate(PermissionSettingActivity.this, R.layout.list_item_permission, null);
            //找到显示的ImageView
            ImageView ivThumb = (ImageView) layout.findViewById(R.id.ivThumb);
            //找到显示名字的TextView
            TextView tvName = (TextView) layout.findViewById(R.id.tvName);
            //获取下标是position的对象
            Permission general = permissions.get(position);
            //设置显示的图像
            ivThumb.setImageResource(general.getImage());
            ivThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //判断权限授权状态
                    boolean b = mPermissionHelper.checkPermission(permissions.get(position).getPermission());
                    //如果没有获取到权限,则尝试获取权限
                    if (!b) {
                        mPermissionHelper.permissionsCheck(permissions.get(position).getPermission(), TSDI_RESULT_CODE);
                    } else {
                        //如果请求成功，则进行相应的操作，比如我们这里打开一个新的Activity
                        Toast.makeText(view.getContext(), getString(R.string.info_permission_start) + permissions.get(position).getPermission(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            //显示名字
            tvName.setText(general.getName());
            return layout;
        }
    }
}
