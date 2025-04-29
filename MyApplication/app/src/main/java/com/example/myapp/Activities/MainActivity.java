// MainActivity.java
package com.example.myapp.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.amap.api.maps.MapsInitializer;
import com.example.myapp.ChatbotFragments.ChatbotFragment;
import com.example.myapp.ProfileFragments.PersonalInfoFragment;

import com.example.myapp.TodayFragments.TodayFragment_google;
import com.example.myapp.HomeFragments.HomeFragment;
import com.example.myapp.ProfileFragments.ProfileFragment;
import com.example.myapp.WalkFragments.WalkFragment;
import com.example.myapp.R;
import com.example.myapp.data.database.AppDatabase;
import com.example.myapp.data.model.User;
import com.example.myapp.tool.UserPreferences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    // 创建各个页面的 Fragment
    private final HomeFragment homeFragment = new HomeFragment();
    private final WalkFragment walkFragment = new WalkFragment();
    private final TodayFragment_google todayFragmentGooglefitness = new TodayFragment_google();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private final ChatbotFragment chatbotFragment = new ChatbotFragment();
    private Fragment currentFragment;
    private UserPreferences userPreferences;
    private String userKey;
    // 底部导航按钮
    private TextView navHome;
    private TextView navToday;
    private TextView navRunning;
    private TextView navProfile;
    private TextView navChatbot;
    private SharedPreferences sharedPreferences;
    // 使用单线程执行器进行后台操作
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.updatePrivacyShow(this,true,true);
        MapsInitializer.updatePrivacyAgree(this,true);
        // 检查登录状态
        if (!isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        } else {
            setContentView(R.layout.activity_home);
        }





        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userKey = sharedPreferences.getString("USER_KEY", null);

        // 初始化数据库（注意：不要在主线程执行查询）
        AppDatabase db = AppDatabase.getDatabase(this);



        // 使用后台线程查询用户数据
        executorService.execute(() -> {
            // 这里执行数据库查询
            final User user = db.userDao().getUserByKey(userKey);
            final boolean exists = (user != null);

            Log.e("inf",user.getUserKey()+","+user.getWeight());
            // 回到主线程更新 UI
            runOnUiThread(() -> {
                if (!exists||user.getWeight()==0) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Tips")
                            .setMessage("You have not filled in the personal information, please fill in now？")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    updateNavigationSelection(R.id.nav_profile, new ProfileFragment());
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.host_fragment_container, new PersonalInfoFragment())
                                            .commit();
                                }
                            })
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                            .show();
                }else
                        {Fragment today = new TodayFragment_google();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                // 添加到指定容器中，并给个标签
                transaction.add(R.id.host_fragment_container, today, "MyFragmentTag");
                // 隐藏该 Fragment
                transaction.hide(today);
                transaction.commit();}
            });
        });

        // 获取底部导航按钮
        navHome = findViewById(R.id.nav_home);
        navToday = findViewById(R.id.nav_today);
        navRunning = findViewById(R.id.nav_walk);
        navProfile = findViewById(R.id.nav_profile);
        navChatbot = findViewById(R.id.nav_gpt);

        navToday.setOnClickListener(v -> updateNavigationSelection(R.id.nav_today, todayFragmentGooglefitness));
        navRunning.setOnClickListener(v -> updateNavigationSelection(R.id.nav_walk, walkFragment));
        navProfile.setOnClickListener(v -> updateNavigationSelection(R.id.nav_profile, profileFragment));
        navChatbot.setOnClickListener(v -> updateNavigationSelection(R.id.nav_gpt, chatbotFragment));
        navHome.setOnClickListener(v -> updateNavigationSelection(R.id.nav_home, homeFragment));

        // 默认加载首页
        updateNavigationSelection(R.id.nav_home, new HomeFragment());



    }

    public void updateNavigationSelection(int navButtonId, Fragment fragment) {
        // 更新按钮颜色
        Log.e("e","111");
        navHome.setTextColor(ContextCompat.getColor(this, R.color.gray));
        navToday.setTextColor(ContextCompat.getColor(this, R.color.gray));
        navRunning.setTextColor(ContextCompat.getColor(this, R.color.gray));
        navProfile.setTextColor(ContextCompat.getColor(this, R.color.gray));
        navChatbot.setTextColor(ContextCompat.getColor(this, R.color.gray));

        TextView selectedButton = findViewById(navButtonId);
        if (selectedButton != null) {
            selectedButton.setTextColor(ContextCompat.getColor(this, R.color.black));
        }

        // 切换 Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.host_fragment_container, fragment)
                .commit();
    }

    // 检查是否已登录
    private boolean isLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_logged_in", false);
    }
}
