package com.skyline.rim.activitys;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qrcode.ScannerActivity;
import com.skyline.rim.adapter.MainFragmentAdapter;
import com.skyline.rim.badgeview.Badge;
import com.skyline.rim.badgeview.QBadgeView;
import com.skyline.rim.controls.TransformFlipHorizontal;
import com.skyline.rim.controls.ViewPagerScroller;
import com.skyline.rim.fragments.Fragment_dongtai;
import com.skyline.rim.fragments.Fragment_gis;
import com.skyline.rim.fragments.Fragment_me;
import com.skyline.rim.fragments.Fragment_mis;
import com.skyline.rim.fragments.WebviewFragment;
import com.skyline.rim.globaldata.systemInfo;
import com.skyline.rim.utils.FileManager;
import com.skyline.rim.utils.StringUtil;
import com.skyline.rim.utils.SystemBarTintManager;
import com.skyline.rim.webapiTools.appInfoChecker;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.controllers.AboutActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_CLEAR_CACHE;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SERVER_STATUS_CHANGED;
import static com.skyline.rim.globaldata.ConstData.HHANDLER_SYS_CHECK;
import static com.skyline.rim.globaldata.ConstData.PARENT_ACTIVITY_GIS;
import static com.skyline.rim.globaldata.systemInfo.bNeedGetBadges;
import static com.skyline.rim.globaldata.systemInfo.bServerAvaible;
import static com.skyline.rim.globaldata.systemInfo.current_fragment;
import static com.skyline.rim.globaldata.systemInfo.serverIP;
import static com.skyline.rim.utils.FileManager.copyFile;
import static com.skyline.rim.utils.FileManager.getMemoryDataPath;
import static com.skyline.rim.utils.NetStatus.isServerConnected;
import static com.skyline.rim.utils.ScreenUtil.getAndroiodScreenWidth;
import static com.skyline.rim.webview.CacheClear.clearCache;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //我   栏目fragment
    private Fragment_me fragmentMe;
    //skyline   栏目fragment
    private Fragment_gis fragmentGis;
    //管理   栏目fragment
    private Fragment_mis fragmentMis;
    //动态   栏目fragment
    private Fragment_dongtai fragmentDongtai;

    //底部tab按钮消息提醒对象
    List<Badge> badges;

    private final int REQUEST_PERMISION_CODE_CAMARE = 0;
    private final int RESULT_REQUEST_CODE = 1;
    private Timer mTimer;
    private TimerTask mTimerTask;

    //声明变量
    private ProgressDialog pd;
    //定义Handler对象接收子线程消息,"初始化"菜单要用到这个异步消息处理
    private Handler handler = new Handler() {
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //只要执行到这里就关闭对话框
            if (pd != null)
                pd.dismiss();
            super.handleMessage(msg);
            switch (msg.what) {
                case HHANDLER_CLEAR_CACHE:
                    break;
                case HHANDLER_SERVER_STATUS_CHANGED:
                    ImageView img = (ImageView) findViewById(R.id.netstatus);
                    if (bServerAvaible == false)
                        img.setVisibility(VISIBLE);
                    else
                        img.setVisibility(GONE);
                    break;
                case 0://请求失败
                    Toast.makeText(MainActivity.this, getString(R.string.info_app_check_error), Toast.LENGTH_LONG).show();
                    break;
                case HHANDLER_SYS_CHECK://请求成功
                    Toast.makeText(MainActivity.this, getString(R.string.info_app_check_finished), Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    //ViewPagerScroller：防止viewpager.setcurrentitem()时，因跨多个页面而导致的闪烁问题
    ViewPagerScroller scroller = null;
    /***
     * 底部tab选项选择事件
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            //current_fragment = item.getItemId();
            Log.i("tsdi", "OnNavigationItemSelectedListener");
            try {
                setFragment(item.getItemId());
            } catch (Exception e) {
                Log.e("tsdi", e.getMessage());
            }
            switch (item.getItemId()) {
                case R.id.dongtai_home:
                    return true;
                case R.id.xcgl_work:
                    return true;
                case R.id.skyline_bim:
                    return true;
                case R.id.account_setting:
                    return true;
            }
            return false;
        }
    };

    /***
     * 初始化底部tab按钮的信息提醒对象
     */
    private void initBadge() {
        badges = new ArrayList<>();
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //获取屏幕宽度
        int screenWidth = getAndroiodScreenWidth(this);
        int itemWidth = screenWidth / navigation.getMenu().size();
        //计算badge的偏移量
        int offsetX = (itemWidth) / 2 - 25;
        if (offsetX < 0)
            offsetX = 0;
        View view = findViewById(R.id.dongtai_home);
        //Badge 数字小于 0 时显示 dot，等于 0 时隐藏整个 Badge，在普通模式下超过 99 时显示 99+，精确模式下显示具体值
        badges.add(new QBadgeView(this).bindTarget(view).setBadgeNumber(0).setGravityOffset(offsetX, 0, true));
        view = findViewById(R.id.xcgl_work);
        badges.add(new QBadgeView(this).bindTarget(view).setBadgeNumber(0).setGravityOffset(offsetX, 0, true));
        view = findViewById(R.id.skyline_bim);
        badges.add(new QBadgeView(this).bindTarget(view).setBadgeNumber(0).setGravityOffset(offsetX, 0, true));
        view = findViewById(R.id.account_setting);
        badges.add(new QBadgeView(this).bindTarget(view).setBadgeNumber(0).setGravityOffset(offsetX, 0, true));
    }

    /***
     * 从服务器读取消息提醒的数量信息并显示到界面
     * @return 是否获取成功
     */
    private boolean autoResetBadges() {
        //Badge 数字小于 0 时显示 dot，等于 0 时隐藏整个 Badge，在普通模式下超过 99 时显示 99+，精确模式下显示具体值
        try {
            badges.get(0).setBadgeNumber(8);
            badges.get(1).setBadgeNumber(0);
            badges.get(2).setBadgeNumber(1);
            badges.get(3).setBadgeNumber(10);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            return false;
        }
        return true;
    }

    //设置窗口的标题
    private void setAppTitle(int type) {
        TextView titleview = (TextView) findViewById(R.id.apptitle);
        switch (type) {
            case 0:
                titleview.setText(this.getString(R.string.dongtai_home));
                break;
            case 1:
                titleview.setText(this.getString(R.string.xcgl_work));
                break;
            case 2:
                titleview.setText(this.getString(R.string.skyline_bim));
                break;
            case 3:
                titleview.setText(this.getString(R.string.account_setting));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bNeedGetBadges)
            if (autoResetBadges())
                bNeedGetBadges = false;
        reSetCurrentFragment();
    }

    /***
     * 回复当前fragment的状态
     */
    private void reSetCurrentFragment() {
        ViewPager pager = (ViewPager) findViewById(R.id.content);
        Log.i("tsdi", "onRestart current_fragment=" + current_fragment + "curreny=" + pager.getCurrentItem());
        if (current_fragment != pager.getCurrentItem())
            setTabFocuse(current_fragment);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        systemInfo.mainContext = null;
        Log.i("tsdi", "onDestroy Main");
        //退出Java程序
        //System.exit(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("tsdi", "main activity  onCreate");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //系统版本大于19
            setTranslucentStatus(true);
        }
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.colorTitleBackgGround);//设置标题栏颜色，此颜色在color中声明

        /*悬浮按钮，暂时去掉
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        //左侧滑动菜单，暂时去掉
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //设置底部tab按钮的颜色
        int[][] states = new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}};
        int[] colors = new int[]{getResources().getColor(R.color.nav_btn_unselected_color), getResources().getColor(R.color.nav_btn_selected_color)};
        ColorStateList csl = new ColorStateList(states, colors);
        navigation.setItemTextColor(csl);
        navigation.setItemIconTintList(csl);

        disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        //初始化各个fragment页面
        initFragments();
        //初始化fragment设置适配器
        ViewPager pager = (ViewPager) findViewById(R.id.content);
        pager.setOffscreenPageLimit(3);
        //初始化适配器
        initPager(pager);
        // 设置切换监听器

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setTabFocuse(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        // 设置ViewPager的切换动画，3.0以上版本有效
        pager.setPageTransformer(false, new TransformFlipHorizontal());
        //处理当前fragment状态异常，当从skyline窗口切回的时候，由于使用了startactivity方法，会是fragment状态变得不正常
        Bundle bundle = new Bundle();
        bundle = this.getIntent().getExtras();
        int parent = 0;
        try {
            parent = bundle.getInt("parent");
        } catch (Exception e) {

        }
        if (parent == PARENT_ACTIVITY_GIS) {
            //从skyline activity切换过来的
            setFragment(R.id.skyline_bim);
        } else
            setAppTitle(0);
        //初始化消息提醒对象
        initBadge();
        systemInfo.mainContext = this;
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (bServerAvaible != isServerConnected(serverIP)) {
                    bServerAvaible = !bServerAvaible;
                    handler.sendEmptyMessage(HHANDLER_SERVER_STATUS_CHANGED);
                }
            }
        };
        mTimer.schedule(mTimerTask, 1000, 2 * 1000);

        //VPN状态显示按钮
        ImageView img = (ImageView) findViewById(R.id.netstatus);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent vpnIntent = new Intent();
                vpnIntent.setAction("android.net.vpn.SETTINGS");
                startActivity(vpnIntent);
            }
        });
    }
    /***
     * 初始化各个fragment页面
     */
    private void initFragments() {
        if (fragmentDongtai == null)
            fragmentDongtai = new Fragment_dongtai();
        if (fragmentMis == null)
            fragmentMis = new Fragment_mis();
        if (fragmentGis == null)
            fragmentGis = new Fragment_gis();
        if (fragmentMe == null)
            fragmentMe = new Fragment_me();
    }

    /***
     * 初始化fragments的适配器
     * @param pager
     */
    private void initPager(ViewPager pager) {
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(fragmentDongtai);
        fragmentList.add(fragmentMis);
        fragmentList.add(fragmentGis);
        fragmentList.add(fragmentMe);
        MainFragmentAdapter adapter = new MainFragmentAdapter(getSupportFragmentManager(), fragmentList);
        pager.setAdapter(adapter);
        //使用自定义scroller控制viewpager跳转多个页面时的动画闪烁问题
        scroller = new ViewPagerScroller(this);
        scroller.initViewPagerScroll(pager);//这个是设置切换过渡时间为2秒 
    }

    public WebviewFragment getCurrentFragment() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        switch (navigation.getSelectedItemId()) {
            case R.id.dongtai_home:
                return fragmentDongtai;
            case R.id.xcgl_work:
                return fragmentMis;
            case R.id.skyline_bim:
                return fragmentGis;
            case R.id.account_setting:
                return fragmentMe;
            default:
                return null;
        }
    }

    // 设置webview回退
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (getCurrentFragment() != null)
//            if (getCurrentFragment().getWebView() != null)
//                if ((keyCode == KeyEvent.KEYCODE_BACK) && getCurrentFragment().getWebView().canGoBack()) {
//                    getCurrentFragment().getWebView().goBack();
//                    return true;
//                }
        //禁止重复启动APP
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (systemInfo.teActivity != null) {
                systemInfo.teActivity.finish();
                systemInfo.teActivity = null;
            }
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;        // a|=b的意思就是把a和b按位或然后赋值给a   按位或的意思就是先把a和b都换成2进制，然后用或操作，相当于a=a|b
        } else {
            winParams.flags &= ~bits;        //&是位运算里面，与运算  a&=b相当于 a = a&b  ~非运算符
        }
        win.setAttributes(winParams);
    }

    //BottomNavigationView在菜单数量大于3个的时候，图标的排列会很怪异，体验并不好，需要通过面的函数禁止掉
    public void disableShiftMode(BottomNavigationView view) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                //noinspection RestrictedApi
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                //noinspection RestrictedApi
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            Log.e("BNVHelper", "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            Log.e("BNVHelper", "Unable to change value of shift mode", e);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //右侧菜单，暂时去掉
        //getMenuInflater().inflate(R.menu.main_right_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle main_bottom_navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_permission) {
            //Permissions23.checkPermission(this,GPS,SD1);
            startActivity(new Intent(this, PermissionSettingActivity.class));
            // Handle the camera action
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            this.startActivity(intent);
//            try{
//                String sdPath=getStoragePath(this,true);
//                if(sdPath==null||sdPath.length()==0)
//                    Log.i("tsdi","no sdcard");
//                else
//                    Log.i("tsdi",sdPath);
//                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/android/data/com.skyline.terraexplorer/skylinedata";
//
//                File directory = new File(path);
//                if (!directory.exists()) {
//                    directory.mkdir();
//                }
//                }catch(Exception e){
//                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
//                }

        } else if (id == R.id.nav_clearhache) {
            //构建一个下载进度条
            pd = ProgressDialog.show(MainActivity.this, getString(R.string.menu_item_clear_cache), getString(R.string.info_app_clearcache_ing));
            new Thread() {
                public void run() {
                    //在这里执行长耗时方法
                    clearCache(getApplicationContext());
                    try {
                        sleep(2000);
                    } catch (Exception e) {

                    }
                    handler.sendEmptyMessage(HHANDLER_CLEAR_CACHE);
                    //执行完毕后给handler发送一个消息

                }
            }.start();
        } else if (id == R.id.nav_exit) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (id == R.id.nav_init_system) {
            //构建一个下载进度条
            pd = ProgressDialog.show(MainActivity.this, getString(R.string.info_app_init), getString(R.string.info_app_init_ing));
            final appInfoChecker checker = new appInfoChecker();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checker.checkAppInfo(getApplicationContext(), handler);
                }
            }, 1000 * 3);
        } else if (id == R.id.nav_open_vpn) {
            Intent vpnIntent = new Intent();
            vpnIntent.setAction("android.net.vpn.SETTINGS");
            startActivity(vpnIntent);
            //copyFile("/data/data/com.skyline.terraexplorer/databases/rim.db", "/storage/emulated/0/skylinedata/rim.db");

        } else if (id == R.id.nav_data_path) {
            Intent intent = new Intent(this, PathSettingActivity.class);
            startActivity(intent);

            Log.i("tsdi", StringUtil.convertGeoDataURL(this, "/aaa/bbb/errt/test.fly"));
        }
        else if (id == R.id.offline_function) {
            Intent intent = new Intent(this, ScannerActivity.class);
            //这里可以用intent传递一些参数，比如扫码聚焦框尺寸大小，支持的扫码类型。
//        //设置扫码框的宽
//        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_WIDTH, 400);
//        //设置扫码框的高
//        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_HEIGHT, 400);
//        //设置扫码框距顶部的位置
//        intent.putExtra(Constant.EXTRA_SCANNER_FRAME_TOP_PADDING, 100);
//        //设置是否启用从相册获取二维码。
//        intent.putExtra(Constant.EXTRA_IS_ENABLE_SCAN_FROM_PIC,true);
//        Bundle bundle = new Bundle();
//        //设置支持的扫码类型
//        bundle.putSerializable(Constant.EXTRA_SCAN_CODE_TYPE, mHashMap);
//        intent.putExtras(bundle);
            startActivityForResult(intent, RESULT_REQUEST_CODE);
//            Intent intent = new Intent(this, WebActivity.class);
//            //获得URL
//            Bundle bundle = new Bundle();
//            bundle.putString("url", "file:///android_asset/error.html");
//            bundle.putInt("functionType", -1);
//            intent.putExtras(bundle);
//            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /***
     * 根据底部tab选项，显示相应的fragment界面
     * @param id 根据底部tab选项的资源ID
     */
    private void setFragment(int id) {
        Log.i("tsdi", "setFragment0");
        ViewPager pager = (ViewPager) findViewById(R.id.content);
        //这个是设置切换过渡时间为0毫秒  
        scroller.setScrollDuration(0);
        //pager.setPageTransformer(false,null);
        setAppTitle(id);
        switch (id) {
            case R.id.dongtai_home:
                pager.setCurrentItem(0);
                current_fragment = 0;
                break;
            case R.id.xcgl_work:
                pager.setCurrentItem(1);
                current_fragment = 1;
                break;
            case R.id.skyline_bim:
                pager.setCurrentItem(2);
                current_fragment = 2;
                break;
            case R.id.account_setting:
                pager.setCurrentItem(3);
                current_fragment = 3;
                break;
        }
        scroller.setScrollDuration(1000);
        Log.i("tsdi", "setFragment1");
    }

    /***
     * 根据fragment的界面顺序，激活底部的tab选项
     * @param position fragment的界面顺序
     */
    private void setTabFocuse(int position) {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        setAppTitle(position);
        //current_fragment=position;
        switch (position) {
            case 0:
            case 1:
            case 2:
            case 3:
                navigation.getMenu().getItem(position).setChecked(true);
                current_fragment = position;
                break;
            default:
                return;
        }
    }

}
