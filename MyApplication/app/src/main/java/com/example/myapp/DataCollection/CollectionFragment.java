package com.example.myapp.DataCollection;

import static androidx.fragment.app.FragmentManager.TAG;
import static com.example.myapp.MapTool.MapContainerView.getAddressFromAPI;
import static com.example.myapp.MapTool.MapContainerView.getNearbyLocations;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.route.WalkStep;
import com.example.myapp.MapTool.MapContainerView;
import com.example.myapp.R;
import com.example.myapp.data.database.AppDatabase;
import com.example.myapp.data.model.Location;
import com.example.myapp.data.model.Route;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CollectionFragment extends Fragment {

    private MapContainerView mapContainerView;
    private FrameLayout mapContainer;
    private ImageView pointerImage;
    private LinearLayout optionsPanel;
    private Button btnSave;
    private Button btnOptions;
    private Button btnView;
    private Button btnDraw;
    private List<Location> locations;


    // 用于保存当前手动规划的所有 Location（构成一个临时路线）
    private List<Location> currentRouteLocations = new ArrayList<>();
    // 当前路线对象，如果为 null 则表示新路线还未开始
    private Route currentRoute = null;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection, container, false);
        mapContainer = view.findViewById(R.id.map_container);
        // 在 Application 的 onCreate 或启动前进行隐私政策配置

        // 在 Application 的 onCreate 或启动前进行隐私政策配置


        try {
            mapContainerView = new MapContainerView(getContext());
            mapContainer.removeAllViews();
            mapContainer.addView(mapContainerView);
            // 仅用于展示，不记录轨迹
            mapContainerView.onCreate();
            // 进入页面时清空地图（移除所有 Marker 和 Polyline）
            mapContainerView.getMapView().getMap().clear();
            mapContainerView.startPureLocation(14f);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "地图加载异常", Toast.LENGTH_SHORT).show();
        }
        ToggleButton toggleSafety = view.findViewById(R.id.toggle_safety);
        ToggleButton toggleInfra = view.findViewById(R.id.toggle_infrastructure);
        pointerImage = view.findViewById(R.id.pointer_image);
        optionsPanel = view.findViewById(R.id.options_panel);
        btnSave = view.findViewById(R.id.btn_save);
        btnOptions = view.findViewById(R.id.btn_options);
        btnView = view.findViewById(R.id.btn_view);
        btnDraw = view.findViewById(R.id.btn_draw);

        optionsPanel.setVisibility(LinearLayout.GONE);
        btnOptions.setOnClickListener(v -> {
            if (optionsPanel.getVisibility() == LinearLayout.GONE) {
                optionsPanel.setVisibility(LinearLayout.VISIBLE);
            } else {
                optionsPanel.setVisibility(LinearLayout.GONE);
            }
        });

        // 加载已有 Location 数据并绘制 Marker（如有需要）
        new Thread(() -> {

            if(AppDatabase.getDatabase(getContext()).routeDao().getAllRoutes().isEmpty())
                AppDatabase.getDatabase(getContext()).locationDao().deleteAll();
            locations = AppDatabase.getDatabase(getContext()).locationDao().getAllLocation();
        }).start();

        // btnSave：保存新 Location 到当前路线，并在地图上标记，同时用高德 API 连接当前所有点
        btnSave.setOnClickListener(v -> {
            // 获取当前地图中心位置
            LatLng center = mapContainerView.getMapView().getMap().getCameraPosition().target;
            double latitude = center.latitude;
            double longitude = center.longitude;
            boolean safety = toggleSafety.isChecked();
            boolean infrastructure = toggleInfra.isChecked();
            new Thread(() -> {
                String addressName = getAddressFromAPI(latitude, longitude);
                if (addressName != null) {
                    // 若当前路线为空，则创建新 Route
                    if (currentRoute == null) {
                        String routeName = "Route " + System.currentTimeMillis();
                        currentRoute = new Route(routeName, 0, 0);
                        long routeId = AppDatabase.getDatabase(getContext()).routeDao().insertRoute(currentRoute);
                        currentRoute.setId(routeId);
                    }
                    // 构造新 Location 对象，将地址名存入 name 属性
                    final Location location = new Location(currentRoute.getId(), addressName, latitude, longitude, safety, infrastructure);
                    long newId = AppDatabase.getDatabase(getContext()).locationDao().insertLocation(location);
                    location.setId(newId);
                    getNearbyLocations(getContext(), latitude, longitude, new MapContainerView.LocationQueryCallback() {
                        @Override
                        public void onQueryComplete(List<Location> locations) {
                            // 查询成功，处理结果
                            if (locations != null && !locations.isEmpty()) {
                                // 处理附近的地点
                                for (Location location : locations) {
                                    Log.e("TAG", "Nearby location: " + location.getName());
                                    location.setPedestrianInfrastructure(infrastructure);
                                    location.setPersonalPerceivedSafety(safety);
                                }
                            }
                        }

                        @Override
                        public void onQueryFailed(String error) {
                            // 查询失败，显示错误消息
                            Log.e("TAG", error);
                        }
                    });

                    currentRouteLocations.add(location);
                    // 后续在地图上标记此 Location
                    requireActivity().runOnUiThread(() -> {
                        mapContainerView.getMapView().getMap().addMarker(
                                new MarkerOptions()
                                        .position(new LatLng(latitude, longitude))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_small_marker))
                        );
                        Toast.makeText(getContext(), "Location saved: " + addressName, Toast.LENGTH_SHORT).show();
                    });
                    // 如果已有上一个点，则用高德 API 绘制两点间的步行路线
                    if (currentRouteLocations.size() > 1) {
                        Location prev = currentRouteLocations.get(currentRouteLocations.size() - 2);
                        drawWalkingRoute(prev, location);
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "逆地理编码失败", Toast.LENGTH_SHORT).show());
                }

            }).start();
        });

        // btnDraw：完成当前路线规划，自动闭合路线（从最后一点到第一个点），更新当前 Route 并清空当前规划
        btnDraw.setOnClickListener(v -> {
            if (currentRouteLocations.isEmpty()) {
                Toast.makeText(getContext(), "当前路线为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentRouteLocations.size() > 1) {
                // 自动闭合路线：连接最后一点和第一点
                drawWalkingRoute(currentRouteLocations.get(currentRouteLocations.size() - 1),
                        currentRouteLocations.get(0));
            }
            // 更新当前 Route 的起点和终点
            currentRoute.setStartLocationId(currentRouteLocations.get(0).getId());
            currentRoute.setEndLocationId(currentRouteLocations.get(currentRouteLocations.size() - 1).getId());
            // （若需要）更新 Route 数据到数据库，此处可调用 updateRoute 方法
            // 清空当前规划，为下一次规划做准备，同时清空地图上的 Marker 和 Polyline
            currentRoute = null;
            currentRouteLocations.clear();
            requireActivity().runOnUiThread(() -> {
                mapContainerView.getMapView().getMap().clear();
            });
        });

        // btnView：查看所有 Route 信息，包括 Route id、包含的 Location 数量及删除按钮
        btnView.setOnClickListener(v -> {
            new Thread(() -> {
                // 查询所有 Route
                List<Route> routes = AppDatabase.getDatabase(getContext()).routeDao().getAllRoutes();
                // 构造一个包含每条 Route 及其 Location 数量的数据集合
                List<String> routeInfoList = new ArrayList<>();
                if (routes != null && !routes.isEmpty()) {
                    for (Route route : routes) {
                        // 注意：这里的数据库查询也在后台线程执行
                        List<Location> locs = AppDatabase.getDatabase(getContext()).routeDao().getLocationsForRoute(route.getId());
                        int count = (locs != null) ? locs.size() : 0;
                        routeInfoList.add("Route ID: " + route.getId() + " | Locations: " + count);
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    LinearLayout listContainer = new LinearLayout(getContext());
                    listContainer.setOrientation(LinearLayout.VERTICAL);
                    int padding = (int) (16 * getResources().getDisplayMetrics().density);
                    listContainer.setPadding(padding, padding, padding, padding);
                    if (!routeInfoList.isEmpty()) {
                        for (int i = 0; i < routes.size(); i++) {
                            Route route = routes.get(i);
                            String info = routeInfoList.get(i);
                            LinearLayout itemLayout = new LinearLayout(getContext());
                            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                            itemLayout.setPadding(0, padding / 2, 0, padding / 2);
                            TextView tv = new TextView(getContext());
                            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                            tv.setText(info);
                            Button btnDelete = new Button(getContext());
                            btnDelete.setText("删除");
                            btnDelete.setOnClickListener(v1 -> {
                                new Thread(() -> {
                                    // 删除 Route，会级联删除对应的 Location（需要数据库配置 onDelete = CASCADE）
                                    AppDatabase.getDatabase(getContext()).routeDao().deleteRoute(route);
                                    requireActivity().runOnUiThread(() -> {
                                        listContainer.removeView(itemLayout);
                                        Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            });
                            itemLayout.addView(tv);
                            itemLayout.addView(btnDelete);
                            listContainer.addView(itemLayout);
                        }
                    } else {
                        TextView tv = new TextView(getContext());
                        tv.setText("No route data found.");
                        listContainer.addView(tv);
                    }
                    ScrollView scrollView = new ScrollView(getContext());
                    scrollView.addView(listContainer);
                    new AlertDialog.Builder(getContext())
                            .setTitle("Route 数据")
                            .setView(scrollView)
                            .setPositiveButton("关闭", null)
                            .show();
                });
            }).start();
        });

        return view;
    }

    /**
     * 利用高德步行路线规划 API，绘制两个 Location 之间的路线
     */
    private void drawWalkingRoute(Location from, Location to) {
        RouteSearch routeSearch = new RouteSearch(getContext());
        // 使用 FromAndTo 封装起点和终点
        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                new LatLonPoint(from.getLatitude(), from.getLongitude()),
                new LatLonPoint(to.getLatitude(), to.getLongitude())
        );
        // 使用默认步行策略
        RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo);
        routeSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onWalkRouteSearched(WalkRouteResult result, int errorCode) {
                if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null &&
                        result.getPaths() != null && !result.getPaths().isEmpty()) {
                    WalkPath path = result.getPaths().get(0);
                    List<LatLng> latLngList = decodeWalkPath(path);
                    requireActivity().runOnUiThread(() -> {
                        mapContainerView.getMapView().getMap().addPolyline(new PolylineOptions()
                                .addAll(latLngList)
                                .color(Color.RED)
                                .width(10));
                    });
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "步行路线规划失败", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onBusRouteSearched(BusRouteResult result, int errorCode) {
                // 空实现
            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
                // 空实现
            }

            @Override
            public void onRideRouteSearched(RideRouteResult result, int errorCode) {
                // 空实现
            }
        });
        routeSearch.calculateWalkRouteAsyn(query);
    }

    /**
     * 辅助方法：将 WalkPath 中所有步骤的坐标转换为 LatLng 集合
     */
    private List<LatLng> decodeWalkPath(WalkPath walkPath) {
        List<LatLng> latLngs = new ArrayList<>();
        if (walkPath == null) return latLngs;
        if (walkPath.getSteps() != null) {
            for (WalkStep step : walkPath.getSteps()) {
                List<LatLonPoint> polyline = step.getPolyline();
                if (polyline != null) {
                    for (LatLonPoint lp : polyline) {
                        latLngs.add(new LatLng(lp.getLatitude(), lp.getLongitude()));
                    }
                }
            }
        }
        return latLngs;
    }

    /**
     * 获取地址的辅助方法（简化版）
     */


    @Override
    public void onResume() {
        super.onResume();
        if (mapContainerView != null) {
            mapContainerView.onResume();
            // 每次进入页面时清空地图
            mapContainerView.getMapView().getMap().clear();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapContainerView != null) {
            // 每次退出页面时清空地图
            mapContainerView.getMapView().getMap().clear();
            mapContainerView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapContainerView != null) {
            mapContainerView.onDestroy();
        }
    }
}
