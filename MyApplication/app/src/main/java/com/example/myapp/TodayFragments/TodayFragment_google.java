package com.example.myapp.TodayFragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ParseException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


import com.example.myapp.data.dao.StepDao;
import com.example.myapp.data.dao.UserDao;
import com.example.myapp.data.database.AppDatabase;
import com.example.myapp.data.model.Step;
import com.example.myapp.data.model.User;
import com.example.myapp.tool.Notification;
import com.example.myapp.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TodayFragment_google extends Fragment {

    private static final String TAG = "TodayFragment";
    private static final int TARGET_STEPS = 10000;
    private static final int RC_SIGN_IN = 9001;
    private final int maxDistance = 3000;


    private View progressContainer;

    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;



    private TextView stepCountTextView;
    private TextView caloriesBurnedTextView;
    private TextView distanceTextView;

    private float userWeight = 70f;
    private AppDatabase appDatabase;
    private StepDao stepDao;
    private UserDao userDao;
    private String userKey;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // 此处使用你创建的 Web OAuth Client ID
    private final String web_client_id = "104629013963-7f9lpcq7g3e26l1v9p4mtnn7rbavkmtv.apps.googleusercontent.com";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(web_client_id)
                .requestEmail()
                .build();
        // 初始化 Google Fit 相关功能（如果有其他初始化操作）
        initializeFitnessFeatures();

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_today_google, container, false);

        progressContainer = rootView.findViewById(R.id.progress_container);
        progressBar = rootView.findViewById(R.id.progressBar);

        stepCountTextView = rootView.findViewById(R.id.step_count_text);
        caloriesBurnedTextView = rootView.findViewById(R.id.calories_burned_text);




        // 修正后的日历控件监听：直接使用选中的 year, month, dayOfMonth 生成日期字符串
        CalendarView calendarView = rootView.findViewById(R.id.calendar_view);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            String selectedDate = sdf.format(cal.getTime());
            Log.e(TAG, "用户选择的日期：" + selectedDate);
            loadDataFromDatabase(selectedDate);
            fetchGoogleFitDataForDate(selectedDate);
        });


        distanceTextView = rootView.findViewById(R.id.distance_text);
        appDatabase = AppDatabase.getDatabase(getContext());
        stepDao = appDatabase.stepDao();
        userDao = appDatabase.userDao();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userKey = sharedPreferences.getString("USER_KEY", "default_user");

        // 默认加载当前日期的数据
        executorService.execute(() -> {
            User user = userDao.getUserByKey(userKey);
            if (user != null) {
                userWeight = user.getWeight();
            }

            if (user == null) {
                // 用户数据不存在，可能需要先创建默认用户或提醒用户填写信息
                Log.e(TAG, "User with key " + userKey + " does not exist.");
                // 例如：创建一个默认用户记录
                User newUser = new User(userKey, "--", 0.0f, 0.0f, 0);
                userDao.insertUser(newUser);
            }
            // 现在再插入 step 数据

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());

            final String currentDate = sdf.format(new Date());
            Log.e(TAG,"默认时间是:" +currentDate );
            Step todayStep = stepDao.getStepByDate(userKey, currentDate);
            final int steps;
            final float distance;
            if (todayStep == null) {
                todayStep = new Step(userKey, currentDate, 0, 0f);
                stepDao.insertStep(todayStep);
                steps = 0;
                distance = 0f;
            } else {
                steps = todayStep.getStepCount();
                distance = todayStep.getDistance();
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateUIWithStepsAndDistance(steps, distance, currentDate));
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signIn();
        } else {
            account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account == null) {
                signIn();
            } else {
                checkAndRequestFitPermissions();
            }
        }

        if (Build.MANUFACTURER.equalsIgnoreCase("HUAWEI")) {
            fetchHuaweiHealthSteps();
        }

        scheduleDailyReset();

        return rootView;
    }

    /**
     * 初始化 Google Fit 相关设置（如果有其他初始化操作，可在此处添加）
     */
    private void initializeFitnessFeatures() {
        // 初始化操作
    }

    /**
     * 先从本地数据库加载指定日期数据，如果没有则从 Google Fit 获取数据
     */

    // 先从数据库加载指定日期的数据
    private void loadDataFromDatabase(String selectedDate) {
        executorService.execute(() -> {
                    Step todayStep = stepDao.getStepByDate(userKey, selectedDate);
                    final int steps;
                    final float distance;
                    if (todayStep == null) {
                        todayStep = new Step(userKey, selectedDate, 0, 0f);
                        stepDao.insertStep(todayStep);
                        steps = 0;
                        distance = 0f;
                    } else {
                        steps = todayStep.getStepCount();
                        distance = todayStep.getDistance();
                    }
            requireActivity().runOnUiThread(() -> updateUIWithStepsAndDistance(steps, distance,selectedDate));
        });
    }

    /**
     * 根据指定日期从 Google Fit 获取数据，并更新 UI 同时存入数据库
     */
    private void fetchGoogleFitDataForDate(String selectedDate) {
        // 计算所选日期的起始和结束时间
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate);
            calendar.setTime(date);
        } catch (ParseException | java.text.ParseException e) {
            e.printStackTrace();
            return;
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        // 结束时间为下一天的凌晨
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        long endTime = calendar.getTimeInMillis();

        Log.d(TAG, "查询时间范围：" + startTime + " - " + endTime);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        GoogleSignInAccount fitAccount = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions);
        if (fitAccount == null) {
            Toast.makeText(getContext(), "未获取到Google登录账户或未授权", Toast.LENGTH_SHORT).show();
            return;
        }

        Fitness.getHistoryClient(requireContext(), fitAccount)
                .readData(readRequest)
                .addOnSuccessListener(dataReadResponse -> {
                    int totalSteps = 0;
                    float totalDistance = 0f;
                    Log.d(TAG, "返回的 Buckets 数量：" + dataReadResponse.getBuckets().size());
                    if (!dataReadResponse.getBuckets().isEmpty()) {
                        for (Bucket bucket : dataReadResponse.getBuckets()) {
                            Log.d(TAG, "处理 Bucket: " + bucket.getStartTime(TimeUnit.MILLISECONDS)
                                    + " - " + bucket.getEndTime(TimeUnit.MILLISECONDS));
                            for (DataSet dataSet : bucket.getDataSets()) {
                                if (dataSet.getDataType().equals(DataType.AGGREGATE_STEP_COUNT_DELTA)) {
                                    totalSteps += extractSteps(dataSet);
                                } else if (dataSet.getDataType().equals(DataType.AGGREGATE_DISTANCE_DELTA)) {
                                    totalDistance += extractDistance(dataSet);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Buckets 为空，直接处理 DataSets");
                        for (DataSet dataSet : dataReadResponse.getDataSets()) {
                            if (dataSet.getDataType().equals(DataType.AGGREGATE_STEP_COUNT_DELTA)) {
                                totalSteps += extractSteps(dataSet);
                            } else if (dataSet.getDataType().equals(DataType.AGGREGATE_DISTANCE_DELTA)) {
                                totalDistance += extractDistance(dataSet);
                            }
                        }
                    }
                    Log.d(TAG, "Google Fit 返回数据：steps=" + totalSteps + ", distance=" + totalDistance);
                    // 更新 UI 和存储数据时传入选择的日期
                    updateUIWithStepsAndDistance(totalSteps, totalDistance, selectedDate);
                    int finalTotalSteps = totalSteps;
                    float finalTotalDistance = totalDistance;
                    executorService.execute(() -> {
                        Step stepRecord = stepDao.getStepByDate(userKey, selectedDate);
                        if (stepRecord == null) {
                            stepRecord = new Step(userKey, selectedDate, finalTotalSteps, finalTotalDistance);
                            stepDao.insertStep(stepRecord);
                        } else {
                            stepRecord.setStepCount(finalTotalSteps);
                            stepRecord.setDistance(finalTotalDistance);
                            stepDao.updateStep(stepRecord);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Google Fit 数据查询失败", e);
                    Toast.makeText(getContext(), "无法获取 Google Fit 数据", Toast.LENGTH_SHORT).show();
                });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount signInAcc = completedTask.getResult(ApiException.class);
            if (signInAcc != null) {
                firebaseAuthWithGoogle(signInAcc);
            }
        } catch (ApiException e) {
            Log.w(TAG, "signInResult: failed code=" + e.getStatusCode(), e);
            updateUI(null);
            Toast.makeText(getContext(), "Google 登录失败或取消", Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "signInWithCredential: success, user=" + user.getEmail());
                            updateUI(GoogleSignIn.getLastSignedInAccount(requireContext()));
                            checkAndRequestFitPermissions();
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential: failure", task.getException());
                        Toast.makeText(getContext(), "Firebase 登录失败", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            Log.d(TAG, "Signed in as " + account.getDisplayName());
            Toast.makeText(getContext(), "Signed in as " + account.getDisplayName(), Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Signed out");
        }
    }

    private void checkAndRequestFitPermissions() {
        if (!isGoogleFitInstalled()) {
            // 未检测到 Google Fit 应用，弹出通知
            Notification.showCustomToast(getContext(), "未检测到 Google Fit 应用，请安装后使用该功能");
            return;
        }

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account == null) {
            signIn();
            return;
        }

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(this, RC_SIGN_IN, account, fitnessOptions);
        }
    }

    /**
     * 检查设备上是否安装了 Google Fit 应用
     */
    private boolean isGoogleFitInstalled() {
        PackageManager pm = requireContext().getPackageManager();
        try {
            pm.getPackageInfo("com.google.android.apps.fitness", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private int extractSteps(DataSet dataSet) {
        int steps = 0;
        for (DataPoint dp : dataSet.getDataPoints()) {
            for (Field field : dp.getDataType().getFields()) {
                steps += dp.getValue(field).asInt();
            }
        }
        return steps;
    }

    private float extractDistance(DataSet dataSet) {
        float distance = 0f;
        for (DataPoint dp : dataSet.getDataPoints()) {
            for (Field field : dp.getDataType().getFields()) {
                distance += dp.getValue(field).asFloat();
            }
        }
        return distance; // 单位通常为米，根据需要可转换为公里（distance / 1000f）
    }

    /**
     * 更新 UI 并将数据存入数据库，注意此处使用传入的 date 参数，确保日期一致
     */
    private void updateUIWithStepsAndDistance(final int steps, final float distance, final String date) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // 显示步行距离（单位：km）
                stepCountTextView.setText(String.format(Locale.getDefault(), "Walking distance：%.2f km", distance / 1000.0));

                // 根据 distance 与 maxDistance 计算百分比（进度条最大值设置为 100）
                float ratio = distance / maxDistance;
                // 限制百分比范围为 0 ~ 100
                int progressPercent = (int) (ratio * 100);
                if (progressPercent > 100) {
                    progressPercent = 100;
                }
                // 更新 ProgressBar 进度
                progressBar.setProgress(progressPercent);

                // 计算消耗的卡路里（示例算法）
                double calories = steps * 0.04 * (userWeight / 70);
                caloriesBurnedTextView.setText(String.format(Locale.getDefault(), "%.2f calories", calories));
                distanceTextView.setText(String.format(Locale.getDefault(), " %,d", steps));
            });
        }
        // 将数据存入数据库，使用传入的 date 参数
        executorService.execute(() -> {
            Step stepRecord = stepDao.getStepByDate(userKey, date);
            if (stepRecord == null) {
                stepRecord = new Step(userKey, date, steps, distance);
                stepDao.insertStep(stepRecord);
            } else {
                stepRecord.setStepCount(steps);
                stepRecord.setDistance(distance);
                stepDao.updateStep(stepRecord);
            }
        });
    }

    private void fetchHuaweiHealthSteps() {
        Toast.makeText(getContext(), "正在使用 Huawei Health 获取步数数据", Toast.LENGTH_SHORT).show();
        // 模拟获取步数数据，此处仅为示例
        int huaweiSteps = queryHuaweiHealthSteps();
        float huaweiDistance = 500f; // 单位：米
        // 这里你可以调用 updateUIWithStepsAndDistance(huaweiSteps, huaweiDistance, <对应日期>) 来更新数据
    }

    private int queryHuaweiHealthSteps() {
        return 7500;
    }

    private void scheduleDailyReset() {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        long initialDelay = calendar.getTimeInMillis() - now;

        PeriodicWorkRequest dailyResetRequest = new PeriodicWorkRequest.Builder(
                com.example.myapp.TodayFragments.StepResetWorker.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(getContext()).enqueueUniquePeriodicWork(
                "daily_step_reset",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyResetRequest
        );
    }
}
