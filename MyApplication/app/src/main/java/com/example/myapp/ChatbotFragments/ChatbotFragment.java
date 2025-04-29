package com.example.myapp.ChatbotFragments;

import static android.view.View.GONE;

import static com.example.myapp.ChatbotFragments.ev1.Top3routes;
import static com.example.myapp.MapTool.MapContainerView.calculateCenter;
import static com.example.myapp.MapTool.MapContainerView.distanceBetween;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.example.myapp.Activities.MainActivity;
import com.example.myapp.MapTool.MapContainerView;
import com.example.myapp.R;
import com.example.myapp.WalkFragments.WalkFragment;
import com.example.myapp.data.dao.StepDao;
import com.example.myapp.data.dao.UserDao;
import com.example.myapp.data.database.AppDatabase;
import com.example.myapp.data.model.Location;
import com.example.myapp.data.model.Route;
import com.example.myapp.data.model.Step;
import com.example.myapp.data.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ChatbotFragment extends Fragment {

    private static final String TAG = "ChatbotFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private LinearLayout routeContainer; // 用于显示地图的容器
    // 控件声明
    private EditText userInput;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    // 路线展示区域

    // 对话辅助类和图片 Uri
    private LatLng userLocation;
    private ChatbotHelper chatbotHelper;
    private Uri photoUri;

    private AMapLocationClient locationClient;

    // 全局对话历史
    private JSONArray conversationHistory = new JSONArray();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chatbot, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userInput = view.findViewById(R.id.user_input);
        Button sendButton = view.findViewById(R.id.send_arrow);
        recyclerView = view.findViewById(R.id.recycler_view_messages);
        routeContainer = view.findViewById(R.id.route_container);

        // 初始化 RecyclerView
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatAdapter);

        // 初始化对话历史
        try {
            conversationHistory.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a helpful assistant."));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        View initialDialog = view.findViewById(R.id.initial_dialog);
        Button optionRoute = view.findViewById(R.id.option_route);
        Button optionReport = view.findViewById(R.id.option_report);

        // “推荐路线”按钮点击事件
        // 在 onViewCreated 中对 optionRoute 按钮的点击事件修改：
        optionRoute.setOnClickListener(v -> {
            // 先显示对话消息
            addChatMessage("Recommend a suitable route for me", true);
            addChatMessage("Okay, based on your current location, I recommend the most suitable route:", false);
            initialDialog.setVisibility(View.GONE);

            try {
                getCurrentLocation(new LocationCallback() {
                    @Override
                    public void onLocationReceived(LatLng location) {
                        userLocation = location;
                        Log.e("ChatbotFragment", "当前定位：" + location.latitude + ", " + location.longitude);
                        // 定位成功后，在后台线程中处理数据库数据
                        new Thread(() -> {
                            List<Route> routes = AppDatabase.getDatabase(getContext()).routeDao().getAllRoutes();
                            // 改为存储 List<List<Location>>
                            List<List<Location>> filteredRouteLocationLists = new ArrayList<>();
                            if (routes != null && !routes.isEmpty()) {
                                for (Route route : routes) {
                                    List<Location> locs = AppDatabase.getDatabase(getContext()).routeDao().getLocationsForRoute(route.getId());
                                    if (locs != null && locs.size() >= 2) {
                                        boolean includeRoute = false;
                                        for (Location loc : locs) {
                                            LatLng locLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                                            if (distanceBetween(userLocation, locLatLng) <= 500) {
                                                includeRoute = true;
                                                break;
                                            }
                                        }
                                        if (includeRoute) {
                                            filteredRouteLocationLists.add(locs);
                                        }
                                    }
                                }
                            }
                            List<List<Location>> finalFilteredRouteLocationLists =Top3routes(filteredRouteLocationLists);
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    // 传入 List<List<Location>> 数据
                                    addBotRouteMessage(finalFilteredRouteLocationLists);
                                    recommendRouteWithExerciseData(finalFilteredRouteLocationLists);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }).start();
                    }

                    @Override
                    public void onLocationFailed(String error) {
                        Log.e("ChatbotFragment", error);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });








        // 初始化 ChatbotHelper（请根据实际情况完善获取                addChatMessage(message, true); API key 的逻辑）
        String apiKey = getApiKeyFromSecureStorage();
        chatbotHelper = new ChatbotHelper(apiKey);

        // 文本消息发送逻辑
        optionReport.setOnClickListener(v -> {
            String message = "My exercise report for this week";
            addChatMessage(message, true);
            initialDialog.setVisibility(GONE);

            new Thread(() -> {
                // 获取用户标识
                String userKey = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .getString("USER_KEY", null);
                AppDatabase db = AppDatabase.getDatabase(getContext());
                StepDao stepDao = db.stepDao();
                UserDao userDao = db.userDao();

                // 获取当前用户体重（kg），若获取失败则默认70kg
                float weight = 70f;
                try {
                    weight = userDao.getUserByKey(userKey).getWeight();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 获取当前日期和过去7天（含今天）的时间范围
                Calendar calendar = Calendar.getInstance();
                Date today = calendar.getTime();
                calendar.add(Calendar.DAY_OF_YEAR, -6);  // 过去7天
                Date startDate = calendar.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // 使用 LinkedHashMap 保持日期顺序记录每天的步数、距离和卡路里
                LinkedHashMap<String, Integer> stepsMap = new LinkedHashMap<>();
                LinkedHashMap<String, Float> distanceMap = new LinkedHashMap<>();
                LinkedHashMap<String, Float> calorieMap = new LinkedHashMap<>();

                // 循环获取过去7天的记录
                Calendar tempCal = Calendar.getInstance();
                tempCal.setTime(startDate);
                while (!tempCal.getTime().after(today)) {
                    String dateStr = sdf.format(tempCal.getTime());
                    Step stepRecord = stepDao.getStepByDate(userKey, dateStr);
                    if (stepRecord != null) {
                        int steps = stepRecord.getStepCount();
                        float distance = stepRecord.getDistance(); // 单位：米
                        // 计算距离转换为公里
                        float distanceKm = distance / 1000f;
                        // 根据公式计算卡路里
                        float calories = distanceKm * weight * 1.036f;

                        stepsMap.put(dateStr, steps);
                        distanceMap.put(dateStr, distance);
                        calorieMap.put(dateStr, calories);
                    } else {
                        stepsMap.put(dateStr, 0);
                        distanceMap.put(dateStr, 0f);
                        calorieMap.put(dateStr, 0f);
                    }
                    tempCal.add(Calendar.DAY_OF_YEAR, 1);
                }

                // 构建汇总信息字符串
                StringBuilder summaryStr = new StringBuilder("Weekly Exercise Summary:\n");
                for (String date : stepsMap.keySet()) {
                    summaryStr.append(date)
                            .append(": Steps = ")
                            .append(stepsMap.get(date))
                            .append(", Distance = ")
                            .append(String.format("%.2f", distanceMap.get(date) / 1000.0))  // 转换为 km
                            .append(" km, Calories = ")
                            .append(String.format("%.2f", calorieMap.get(date)))
                            .append(" cal\n");
                }

                // 在主线程更新 UI，展示汇总报告
                requireActivity().runOnUiThread(() -> addChatMessage(summaryStr.toString(), false));

                // 拼接完整消息，发送给 GPT 进行数据分析（要求答案在 100 tokens 内）
                String fullMessage = "Based on the following weekly exercise data:\n"
                        + summaryStr.toString()
                        + "Please analyze my performance and suggest improvements (answer within 100 tokens).";
                requireActivity().runOnUiThread(() -> {
                    chatbotHelper.sendMessage(fullMessage, conversationHistory, new ChatbotResponseListener() {
                        @Override
                        public void onResponse(String reply) {
                            addChatMessage(reply, false);
                        }
                        @Override
                        public void onFailure(String error) {
                            addChatMessage("Failed to connect to Chatbot: " + error, false);
                        }
                    });
                });
            }).start();
        });

        sendButton.setOnClickListener(v -> {
            String userMessage = userInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addChatMessage(userMessage, true);  // 添加用户消息到对话历史
                // 清空输入框
                userInput.setText("");
                // 添加对话消息到历史记录
                try {
                    conversationHistory.put(new JSONObject()
                            .put("role", "user")
                            .put("content", userMessage));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 发送消息给 GPT
                chatbotHelper.sendMessage(userMessage, conversationHistory, new ChatbotResponseListener() {
                    @Override
                    public void onResponse(String reply) {
                        addChatMessage(reply, false); // 添加 GPT 的回复
                    }

                    @Override
                    public void onFailure(String error) {
                        addChatMessage("Failed to connect to Chatbot: " + error, false);
                    }
                });
            }
        });


        // 拍照按钮点击事件
        Button sendPhoto = view.findViewById(R.id.send_photo);
        sendPhoto.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
    }




    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), "相机权限被拒绝，无法拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred while creating the image file", ex);
            }
            if (photoFile != null) {
                try {
                    photoUri = FileProvider.getUriForFile(requireContext(),
                            "com.example.myapp.fileprovider", photoFile);
                } catch (Exception e) {
                    Log.e(TAG, "FileProvider.getUriForFile exception", e);
                    return;
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.e(TAG, "photoFile is null");
            }
        } else {
            Log.e(TAG, "No camera app available");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(null);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            chatbotHelper.sendPhotoMessage(photoUri.toString(), requireContext(), new ChatbotResponseListener() {
                @Override
                public void onResponse(String reply) {
                    addChatMessage(reply, false);
                }
                @Override
                public void onFailure(String error) {
                    addChatMessage("Failed to connect to Chatbot (Photo): " + error, false);
                }
            });
        }
    }







    @SuppressLint("SetTextI18n")
    private void addBotRouteMessage(List<List<Location>>   routes) throws Exception {
        // 加载自定义布局作为消息内容
        View routeMessageView = LayoutInflater.from(getContext())
                .inflate(R.layout.bot_route_message, recyclerView, false);
        LinearLayout container = routeMessageView.findViewById(R.id.route_maps_container);


        // 遍历每条路线
        for (int i = 0; i < 3; i++) {//如果附近路线超过3个可能还要加一个排序把得分高的排前面
            final List<Location> routeLocations = routes.get(i);
            // 创建一个容器用于展示该路线预览及按钮
            RelativeLayout routeContainer = new RelativeLayout(getContext());
            int containerHeight = (int) (200 * getResources().getDisplayMetrics().density);
            RelativeLayout.LayoutParams routeContainerParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, containerHeight);
            routeContainer.setLayoutParams(routeContainerParams);
            routeContainer.setPadding(8, 8, 8, 8);
            routeContainer.setBackgroundColor(Color.TRANSPARENT);

            // 将 List<Location> 转换为 List<LatLng>
            List<LatLng> latLngList = new ArrayList<>();
            for (Location loc : routeLocations) {
                latLngList.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
            }

            // 创建 MapContainerView 实例并绘制路线预览
            MapContainerView mapView = new MapContainerView(getContext());
            RelativeLayout.LayoutParams mapParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    (int) (150 * getResources().getDisplayMetrics().density));
            mapParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mapView.setLayoutParams(mapParams);

            mapView.onCreate();
            mapView.drawRoute(latLngList,Color.RED);

            // 禁用地图手势（预览不交互）
            if (mapView.getMapView() != null && mapView.getMapView().getMap() != null) {
                AMap aMap = mapView.getMapView().getMap();
                aMap.getUiSettings().setZoomControlsEnabled(false);
                aMap.getUiSettings().setScrollGesturesEnabled(false);
                aMap.getUiSettings().setZoomGesturesEnabled(false);
            }
            routeContainer.addView(mapView);

            // 添加文本标签显示路线编号和评分
            TextView routeLabel = new TextView(getContext());
            RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            labelParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            labelParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            routeLabel.setLayoutParams(labelParams);
            String userKey = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("USER_KEY", null);
            double score = ev1.evaluateRouteScore(routeLocations, userKey);
            routeLabel.setText(String.format("Route %d: %.2f", i + 1, score));
            routeLabel.setTextColor(getResources().getColor(android.R.color.white));
            routeLabel.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            routeLabel.setPadding(4, 4, 4, 4);
            routeContainer.addView(routeLabel);

            // 添加 "View Details" 按钮，点击后跳转到 WalkFragment
            Button WalkButton = new Button(getContext());
            WalkButton.setText("Start navigation");
            WalkButton.setTextSize(12);  // 设置字体大小，单位为sp

            // 设置按钮的宽高
            RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            btnParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            btnParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

            // 可以根据需要调整按钮的宽度和高度（单位为dp）
            int buttonWidth = (int) (160 * getResources().getDisplayMetrics().density);  // 设置宽度
            int buttonHeight = (int) (40 * getResources().getDisplayMetrics().density); // 设置高度

            btnParams.width = buttonWidth;
            btnParams.height = buttonHeight;
            WalkButton.setLayoutParams(btnParams);
            WalkButton.setOnClickListener(v -> {
                if (isAdded() && getActivity() instanceof MainActivity) {

                    // 创建一个 Bundle 来传递 Route 数据
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList("route_data", new ArrayList<>(routeLocations)); // 假设 Route 实现了 Parcelable 接口

                    // 创建 WalkFragment 并传递数据
                    WalkFragment walkFragment = new WalkFragment();
                    walkFragment.setArguments(bundle);
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.updateNavigationSelection(R.id.nav_walk, walkFragment);
                }
            });
            routeContainer.addView(WalkButton);

            // 在地图加载完成后，使用 calculateCenter 设置镜头中心为该路线的平均中心点
            mapView.postDelayed(() -> {
                if (mapView.getMapView() != null && mapView.getMapView().getMap() != null) {
                    AMap aMap = mapView.getMapView().getMap();
                    LatLng center = calculateCenter(latLngList);
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(center)
                            .zoom(14)
                            .tilt(30)
                            .bearing(0)
                            .build();
                    aMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }, 200);

            container.addView(routeContainer);
        }
        addChatMessage(routeMessageView, false);
    }









    /**
     * 辅助方法：计算一组坐标的中心点
     */






    private void addChatMessage(View view, boolean isUser) {
        // 假设你的 Message 类支持存储一个 View 类型的消息内容
        Message msg = new Message(view, isUser); // 修改 Message 类构造方法支持 View 类型内容
        messageList.add(msg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.smoothScrollToPosition(messageList.size() - 1);
    }
    /**
     * 将消息添加到 RecyclerView 并更新界面
     */
    private void addChatMessage(String text, boolean isUser) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            messageList.add(new Message(text, isUser));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
        });
    }

    private String convertRouteToDescription(List<Location> route) {
        if (route == null || route.isEmpty()) return "No valid data";

        // 计算总距离（单位：米）
        double totalDistance = 0;
        for (int i = 1; i < route.size(); i++) {
            Location prev = route.get(i - 1);
            Location curr = route.get(i);
            LatLng p1 = new LatLng(prev.getLatitude(), prev.getLongitude());
            LatLng p2 = new LatLng(curr.getLatitude(), curr.getLongitude());
            totalDistance += distanceBetween(p1, p2);
        }

        // 转换为公里
        double totalDistanceKm = totalDistance / 1000.0;

        // 假设步行速度为 5 km/h，计算预计耗时（分钟）
        double duration = totalDistanceKm / 5.0 * 60;

        // 返回路线描述：总距离和预计耗时
        return String.format("Total distance: %.2f km, Estimated time: %.0f minutes", totalDistanceKm, duration);
    }



    /**
     * 推荐路线：先查询用户近一周的步行数据，再整合路线描述信息，
     * 然后拼接成一条综合消息传递给 Chatbot 获取推荐结果
     */
    private void recommendRouteWithExerciseData(List<List<Location>> routes) {
        // 直接从 SharedPreferences 中获取 userKey
        String userKey = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("USER_KEY", null);

        // 整理每条路线的描述信息，并附带评分
        StringBuilder routesDesc = new StringBuilder();
        for (int i = 0; i < routes.size(); i++) {
            List<Location> routeLocations = routes.get(i);
            // 通过 ev1.evaluateRouteScore 计算该路线的评分
            double score = ev1.evaluateRouteScore(routeLocations, userKey);
            // 生成路线描述（例如总距离和预计耗时）
            String desc = convertRouteToDescription(routeLocations);
            Log.e("desc",desc);
            routesDesc.append("Route ").append(i + 1).append(": ").append(desc)
                    .append(" - Score: ").append(String.format("%.2f", score)).append("；");
        }

        // 在后台线程中查询用户近7天的步行数据
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(getContext());
            StepDao stepDao = db.stepDao();
            UserDao userDao = db.userDao();
            List<Step> allSteps = stepDao.getAllStepsByUserId(userKey);
            User user = userDao.getUserByKey(userKey);
            float weight = user.getWeight();

            // 计算日期范围：近7天
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            Date weekAgo = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            int totalSteps = 0;
            float totalDistance = 0f;
            String todayDate = sdf.format(today);
            Step stepRecord = stepDao.getStepByDate(userKey, todayDate);


            float distanceToday=0f;
            if (stepRecord != null) {

                distanceToday = stepRecord.getDistance(); // 单位：米
            };

            for (Step s : allSteps) {
                try {
                    Date stepDate = sdf.parse(s.getDate());
                    if (stepDate != null && !stepDate.before(weekAgo)) {
                        totalSteps += s.getStepCount();
                        totalDistance += s.getDistance();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            String exerciseData = "Total steps: " + totalSteps + " steps, Total distance: " + totalDistance / 1000 + " km, Weight: " + weight + "kg";


            String explanation = "Note: The route score represents the probability of choosing to walk.";


            String fullMessage = "Based on my exercise data from the past week: " + exerciseData +
                    ", and the available routes: " + routesDesc.toString() +
                    " " + explanation +
                    "if my distance walked today:"+distanceToday+" add the distance of certain route can reach my average distance of a week, please give the route a higher priority if the score isn't too low"+
                    " Please recommend the most suitable route to me according my personal exerciseData and tell me distance and time will cost for the route"+
                    "and explain why (answer within 100 tokens).";

            requireActivity().runOnUiThread(() -> {
                chatbotHelper.sendMessage(fullMessage, conversationHistory, new ChatbotResponseListener() {
                    @Override
                    public void onResponse(String reply) {
                        addChatMessage(reply, false);
                    }
                    @Override
                    public void onFailure(String error) {
                        addChatMessage("Failed to connect to Chatbot: " + error, false);
                    }
                });
            });
        }).start();
    }



    public interface LocationCallback {
        void onLocationReceived(LatLng location);
        void onLocationFailed(String error);
    }

    // 在 ChatbotFragment 内部添加 getCurrentLocation 方法
    private void getCurrentLocation(LocationCallback callback) throws Exception {


        // 在 getCurrentLocation 方法之前调用
        AMapLocationClient.updatePrivacyShow(getContext(), true, true);
        AMapLocationClient.updatePrivacyAgree(getContext(), true);


        AMapLocationClient locationClient = new AMapLocationClient(getContext());
        AMapLocationClientOption option = new AMapLocationClientOption();
        option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        option.setOnceLocation(true); // 一次定位
        locationClient.setLocationOption(option);
        locationClient.setLocationListener(aMapLocation -> {
            if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                // 定位成功，构造当前经纬度
                LatLng currentLocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                callback.onLocationReceived(currentLocation);
            } else {
                String errMsg = aMapLocation != null ? aMapLocation.getErrorInfo() : "Unknown error";
                callback.onLocationFailed("定位失败：" + errMsg);
            }
            // 定位完成后停止并销毁定位客户端
            locationClient.stopLocation();
            locationClient.onDestroy();
        });
        locationClient.startLocation();
    }




    /**
     * 模拟从安全存储中获取 API 密钥的逻辑（请根据实际情况修改）
     */
    private String getApiKeyFromSecureStorage() {
        return "sk-O62I7CQRETZ1dSFevmJWqdsJtsfWmg91sbBdWY8tJDRbgYTm";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (routeContainer != null) {
            // 遍历容器内的所有子视图，找到 MapContainerView 并调用其 onDestroy 方法
            for (int i = 0; i < routeContainer.getChildCount(); i++) {
                View child = routeContainer.getChildAt(i);
                if (child instanceof MapContainerView) {
                    ((MapContainerView) child).onDestroy();
                }
            }
            routeContainer.removeAllViews();
        }
    }

}
