package com.example.myapp.HomeFragments;

import static com.example.myapp.MapTool.MapContainerView.getAddressFromAPI;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.example.myapp.R;
import com.example.myapp.data.database.AppDatabase;
import com.example.myapp.data.dao.RouteDao;
import com.example.myapp.data.model.Location;
import com.example.myapp.data.model.Route;
import com.example.myapp.MapTool.MapContainerView;
import com.example.myapp.ChatbotFragments.ev1;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RouteFragment extends Fragment {

    private LinearLayout routeContainer;
    private AppDatabase appDatabase;
    private RouteDao routeDao;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 布局文件 fragment_route.xml 内包含一个 id 为 route_container 的 LinearLayout（用 ScrollView 包裹以实现上下滑动）
        return inflater.inflate(R.layout.fragment_route, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        routeContainer = view.findViewById(R.id.route_container);
        appDatabase = AppDatabase.getDatabase(getContext());
        routeDao = appDatabase.routeDao();
        loadAndDisplayRoutes();
    }

    private void loadAndDisplayRoutes() {
        new Thread(() -> {
            List<Route> routes = routeDao.getAllRoutes();
            if (routes != null && !routes.isEmpty()) {
                for (Route route : routes) {
                    // 获取当前 Route 对应的所有 Location 数据
                    List<Location> locs = routeDao.getLocationsForRoute(route.getId());
                    if (locs != null && locs.size() >= 2) {
                        // 检查并更新空的地点名称
                        updateEmptyLocationNames(locs);
                        getActivity().runOnUiThread(() -> addRouteView(locs));
                    }
                }
            }
        }).start();
    }

    /**
     * 遍历传入的 Location 列表，如果发现 name 属性为空，
     * 则调用逆地理编码方法获取地址名称，并更新 Location 对象及数据库记录。
     */
    private void updateEmptyLocationNames(List<Location> locations) {
        boolean hasUpdate = false;
        for (Location loc : locations) {
            if (loc.getName() == null || loc.getName().trim().isEmpty()) {
                String addressName = getAddressFromAPI(loc.getLatitude(), loc.getLongitude());
                if (addressName != null && !addressName.isEmpty()) {
                    loc.setName(addressName);
                    // 更新数据库中该 Location 对象
                    appDatabase.locationDao().updateLocation(loc);
                    hasUpdate = true;
                }
            }
        }
        if (hasUpdate) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "更新空地址名称成功", Toast.LENGTH_SHORT).show());
        }
    }

    private void addRouteView(List<Location> locs) {
        // 创建一个容器存放地图预览和标题
        RelativeLayout routeItem = new RelativeLayout(getContext());
        int itemHeight = (int) (250 * getResources().getDisplayMetrics().density);
        RelativeLayout.LayoutParams itemParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, itemHeight);
        routeItem.setLayoutParams(itemParams);
        routeItem.setPadding(8, 8, 8, 8);

        // 将 Location 列表转换为 LatLng 列表
        List<LatLng> latLngList = new ArrayList<>();
        for (Location loc : locs) {
            latLngList.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        }

        // 创建 MapContainerView 实例并为其动态生成一个 id
        MapContainerView mapView;
        try {
            mapView = new MapContainerView(getContext());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int mapViewId = View.generateViewId();
        mapView.setId(mapViewId);

        RelativeLayout.LayoutParams mapParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                (int) (150 * getResources().getDisplayMetrics().density));
        mapParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mapView.setLayoutParams(mapParams);

        mapView.onCreate();
        //绘制路线
        mapView.drawRoute(latLngList,Color.BLUE);

        // 禁用地图手势，使预览图不可交互
        if (mapView.getMapView() != null && mapView.getMapView().getMap() != null) {
            AMap aMap = mapView.getMapView().getMap();
            aMap.getUiSettings().setZoomControlsEnabled(false);
            aMap.getUiSettings().setScrollGesturesEnabled(false);
            aMap.getUiSettings().setZoomGesturesEnabled(false);
        }
        routeItem.addView(mapView);

        // 添加标题 TextView，放在地图预览下方
        String placeName = locs.get(0).getName();
        if (placeName == null || placeName.trim().isEmpty()) {
            placeName = "未知地点";
        }

        double score = ev1.evaluateRouteScore(locs, requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("USER_KEY", null));
        String title = String.format("%s - Score: %.2f", placeName, score);
        TextView titleView = new TextView(getContext());
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        titleParams.addRule(RelativeLayout.BELOW, mapViewId);
        titleParams.setMargins(0, 8, 0, 0); // 设置上边距，避免与地图预览紧贴
        titleView.setLayoutParams(titleParams);
        titleView.setText(title);
        titleView.setTextColor(Color.BLACK);
        titleView.setTextSize(16);
        routeItem.addView(titleView);

        // 在地图加载完成后，使用 calculateCenter 设置镜头中心为该路线的平均中心位置
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

        routeContainer.addView(routeItem);
    }


    /**
     * 计算一组 LatLng 点的平均中心位置
     */
    private LatLng calculateCenter(List<LatLng> points) {
        double sumLat = 0;
        double sumLng = 0;
        for (LatLng point : points) {
            sumLat += point.latitude;
            sumLng += point.longitude;
        }
        int count = points.size();
        return new LatLng(sumLat / count, sumLng / count);
    }
}
