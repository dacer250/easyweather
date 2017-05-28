package com.weather.byhieg.easyweather.home;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClientOption;
import com.example.byhieglibrary.Activity.BaseActivity;
import com.example.byhieglibrary.Utils.DateUtil;
import com.example.byhieglibrary.Utils.DisplayUtil;
import com.example.byhieglibrary.Utils.LogUtils;
import com.weather.byhieg.easyweather.Activity.CityManageActivity;
import com.weather.byhieg.easyweather.Activity.LoveAppActivity;
import com.weather.byhieg.easyweather.Activity.SlideMenuActivity;
import com.weather.byhieg.easyweather.Adapter.DrawerListAdapter;
import com.weather.byhieg.easyweather.Adapter.PopupWindowAdapter;
import com.weather.byhieg.easyweather.Bean.DrawerContext;
import com.weather.byhieg.easyweather.Bean.HoursWeather;
import com.weather.byhieg.easyweather.Bean.WeatherBean;
import com.weather.byhieg.easyweather.Interface.MyItemClickListener;
import com.weather.byhieg.easyweather.MyApplication;
import com.weather.byhieg.easyweather.R;
import com.weather.byhieg.easyweather.startweather.NotificationService;
import com.weather.byhieg.easyweather.data.source.local.entity.LoveCityEntity;
import com.weather.byhieg.easyweather.tools.Constants;
import com.weather.byhieg.easyweather.tools.HandleDaoData;
import com.weather.byhieg.easyweather.tools.WeatherJsonConverter;
import com.weather.byhieg.easyweather.tools.MyLocationListener;
import com.weather.byhieg.easyweather.tools.NetTool;
import com.weather.byhieg.easyweather.tools.WeatherIcon;
import com.weather.byhieg.easyweather.customview.WeekWeatherView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;

import static com.example.byhieglibrary.Utils.DisplayUtil.getViewHeight;
import static com.weather.byhieg.easyweather.R.id.swipe_refresh;

public class MainActivity extends BaseActivity implements ActivityCompat
        .OnRequestPermissionsResultCallback,HomeFragment.Callback {


    @BindView(R.id.toolbar)
    public Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    public DrawerLayout drawerLayout;
    @BindView(R.id.nav_view)
    public NavigationView mNavigationView;


    private BDLocationListener myListener;
    private HomePresenter mHomePresenter;
    private FragmentManager fm;

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initData() {
        fm = getSupportFragmentManager();
        HomeFragment homeFragment = (HomeFragment) fm.findFragmentById(R.id.fragment_container);
        if (homeFragment == null) {
            homeFragment = HomeFragment.newInstance();
            fm.beginTransaction().add(R.id.fragment_container,homeFragment).commit();
        }

        mHomePresenter = new HomePresenter(homeFragment);
        myListener = new MyLocationListener(this);
        MyApplication.getmLocationClient().registerLocationListener(myListener);

//


    }

    @Override
    public void initView() {
//        generateTextView();
//        lineChart.invalidate();
        toolbar.setTitle("成都");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, 0, 0);
        mDrawerToggle.syncState();
        drawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    public void initTheme() {
        if(MyApplication.nightMode()){
            setTheme(R.style.NightTheme);
        }else {
            setTheme(R.style.DayTheme);
        }
    }

    @Override
    public void initEvent() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.feedback:
                        AlertDialog.Builder dialogBuilder;
                        if (MyApplication.nightMode2()){
                           dialogBuilder  = new AlertDialog.Builder(MainActivity.this, R.style.NightDialog);
                        }else{
                            dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                        }
                       dialogBuilder.setTitle("反馈").setMessage("在使用过程中，有任何问题均可以发送到邮箱：byhieg@gmail.com").setPositiveButton("恩", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
                        break;

                    case R.id.location:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED||
                                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                LogUtils.e("Permissions","还是没有权限啊");
                                // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义)
                                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_WIFI_STATE,
                                        Manifest.permission.ACCESS_NETWORK_STATE,
                                        Manifest.permission.CHANGE_WIFI_STATE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, Constants.PERMISSION);
                            }else{
                                mHomePresenter.doBaiduLocation();
                                LogUtils.e("Permissions","已经有权限了");
                            }
                        }else{
                            mHomePresenter.doBaiduLocation();
                        }

                        break;

                    case R.id.like:
                        startActivity(LoveAppActivity.class);
                        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                        break;
                    case R.id.add_city:
                        startActivity(CityManageActivity.class);
                        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                        break;
                }
                return true;
            }
        });

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int postion = 0;
                switch (menuItem.getItemId()) {
                    case R.id.trending:
                        break;
                    case R.id.setting:
                        postion = 1;
                        break;
                    case R.id.share:
                        postion = 2;
                        break;
                    case R.id.help:
                        postion = 3;
                        break;
                    case R.id.lab:
                        postion = 4;
                        break;
                    case R.id.wiki:
                        postion = 5;
                        break;
                    case R.id.more:
                        postion = 6;
                        break;
                }
                menuItem.setChecked(true);
                drawerLayout.closeDrawers();
                Intent intent = new Intent(getApplicationContext(), SlideMenuActivity.class);
                intent.putExtra("itemId", postion);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                return true;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


//    public void generateTextView() {
//        TextView textView = new TextView(this);
//        textView.setText("天气易变，注意天气变化");
//        View[] view = {findViewById(R.id.toolbar), findViewById(R.id.view), findViewById(R.id.item_cloths), findViewById(R.id.item_sports)};
//        int totalHeight = 0;
//        for (View aView : view) {
//            totalHeight += getViewHeight(aView, true) + DisplayUtil.dip2px(this, 10);
//        }
//        int pxHeight = getmScreenHeight() - totalHeight;
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pxHeight / 2);
//        textView.setGravity(Gravity.CENTER);
//        textView.setTextColor(ContextCompat.getColor(this, R.color.white));
//        textView.setLayoutParams(lp);
//        action_bar.addView(textView);
//
//    }

    @Override
    protected void onResume() {
        super.onResume();

        if (MyApplication.nightMode2()) {
            initNightView(R.layout.night_mode_overlay);
        } else {
            removeNightView();
        }
    }


    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        option.setScanSpan(1000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        MyApplication.getmLocationClient().setLocOption(option);
    }






    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);
        switch(requestCode)
        {
            // requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case Constants.PERMISSION:
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // 获取到权限，作相应处理（调用定位SDK应当确保相关权限均被授权，否则可能引起定位失败）
                MyApplication.getmLocationClient().start();
            }
            else
            {
                // 没有获取到权限，做特殊处理
                showToast("没有权限，定位失败");
            }
            break;
            default:
                break;
        }
    }

    @Override
    public void updateToolBar(String cityName) {
        toolbar.setTitle(cityName);
    }
    
}