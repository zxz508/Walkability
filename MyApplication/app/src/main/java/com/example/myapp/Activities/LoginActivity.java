package com.example.myapp.Activities;



import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapp.R;
import com.example.myapp.tool.Notification;
import com.example.myapp.tool.UserPreferences;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {


    private EditText usernameEditText;
    private EditText passwordEditText;
    private UserPreferences userPreferences;



    private LocationManager locationHelper;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private TextView locationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        userPreferences = new UserPreferences(this);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);
        Notification.showCustomToast(this,"");



        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                //test only!!!!!!!!!!!!!!!11
                Log.e("log","111");
                if(login_verify(username,password)){
                    Log.e("log","2");
                    saveLoginStatus(true, username);
                    userPreferences.generateAndSaveUserKey();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }


            }
        });
        // 初始化权限请求器
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        // 权限已授予，初始化定位
                        // 权限被拒绝，显示全屏对话框并退出应用
                        showPermissionDeniedDialog();
                    }
                }
        );

        // 检查和请求权限
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
        }
    }

    private void saveLoginStatus(boolean isLoggedIn, String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", isLoggedIn);
        editor.putString("username", username);  // 可以保存用户名或其他信息
        editor.apply();
    }

    /**
     * 检查是否已授予ACCESS_FINE_LOCATION权限
     */
    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求ACCESS_FINE_LOCATION权限
     */
    private void requestLocationPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean login_verify(String username, String password){
        Log.e("log",username+","+password);
        if(Objects.equals(username, "test") && Objects.equals(password, "123")) return true;
        else return false;

    }

    /**
     * 初始化定位
    */


    /**
     * 判断位置是否精确
     */


    /**
     * 显示权限被拒绝的全屏对话框，并退出应用
     */


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止位置更新，防止内存泄漏

    }

    /**
     * 请求ACCESS_FINE_LOCATION权限
     */

    public void showPermissionDeniedDialog() {
        // 使用自定义布局加载对话框
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_fullscreen_permission, null);

        // 创建AlertDialog并应用自定义样式
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // 获取对话框中的视图组件
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button buttonSettings = dialogView.findViewById(R.id.dialog_button_settings);
        Button buttonExit = dialogView.findViewById(R.id.dialog_button_exit);

        // 设置按钮点击事件
        buttonSettings.setOnClickListener(v -> {
            dialog.dismiss();
            // 引导用户前往应用设置页面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        buttonExit.setOnClickListener(v -> {
            dialog.dismiss();
            // 退出应用
            finishAffinity();
        });

        // 显示对话框
        dialog.show();
    }



}
