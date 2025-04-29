package com.example.myapp.ChatbotFragments;

import static com.example.myapp.MapTool.MapContainerView.distanceBetween;

import com.amap.api.maps.model.LatLng;
import com.example.myapp.data.model.Location;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ev1 {

    // 计算两个 LatLng 之间的距离（单位：米），使用 Haversine 公式


    /**
     * 评估一条路线的得分，基于论文中 Model 1 参数
     * @param route 该路线对应的 Location 列表（要求至少包含两个地点）
     * @param userKey 用户的唯一标识符（例如：用户性别等信息）
     * @return 步行概率（0～1），得分越高表示该路线越适合步行
     */
    public static double evaluateRouteScore(List<Location> route, String userKey) {
        if(route == null || route.size() < 2) {
            return 0;
        }

        // 计算总路长，排除相邻重复点（距离小于10米的视为重复）
        double totalLength = 0;
        double minDistanceThreshold = 10; // 米
        for (int i = 1; i < route.size(); i++) {
            Location prev = route.get(i - 1);
            Location curr = route.get(i);
            LatLng p1 = new LatLng(prev.getLatitude(), prev.getLongitude());
            LatLng p2 = new LatLng(curr.getLatitude(), curr.getLongitude());
            double d = distanceBetween(p1, p2);
            if (d >= minDistanceThreshold) {
                totalLength += d;
            }
        }
        // 转换为公里
        double walkDistance = totalLength / 1000.0;

        // 计算平均安全性和基础设施得分
        double safetySum = 0;
        double infraSum = 0;
        for (Location loc : route) {
            safetySum += loc.isPersonalPerceivedSafety() ? 1 : 0;
            infraSum += loc.isPedestrianInfrastructure() ? 1 : 0;
        }
        double avgSafety = safetySum / route.size();
        double avgInfra = infraSum / route.size();

        // 用户性别转换：female为1，male为0
        int gender = "female".equalsIgnoreCase(userKey) ? 1 : 0;

        // 计算效用（按照论文 Model 1 参数）
        double utility = 0.502
                - 0.332 * walkDistance  // 步行距离的系数
                + 0.632 * getIncentive(walkDistance)  // 经济激励设为0
                + 0.809 * avgInfra
                + 1.030 * avgSafety
                - 0.386 * gender;  // 根据性别调整

        // 将效用转换为概率
        return Math.exp(utility) / (1 + Math.exp(utility));
    }


    private static double getIncentive(double walkDistance) {
        if (walkDistance >= 1&&walkDistance<2) {
            return 0.38;  // 步行距离 1 公里以下，激励为 0.5 欧元
        } else if (walkDistance <3) {
            return 0.38*2;  // 步行距离 1 到 2 公里，激励为 1.0 欧元
        } else {
            return 0.38*3;  // 步行距离 2 公里以上，激励为 1.5 欧元
        }
    }

    public static List<List<Location>> Top3routes(List<List<Location>> routes) {
        // 创建一个列表来保存每条路线及其对应的得分
        List<Map.Entry<List<Location>, Double>> routeScores = new ArrayList<>();

        // 遍历每条路线并计算其得分
        for (List<Location> route : routes) {
            double score = ev1.evaluateRouteScore(route, "userKey");  // 假设传递的 userKey 为已脱敏的数据
            routeScores.add(new AbstractMap.SimpleEntry<>(route, score));
        }

        // 对路线根据得分进行排序，得分从高到低
        routeScores.sort((r1, r2) -> Double.compare(r2.getValue(), r1.getValue()));

        // 选取前三条得分最高的路线
        List<List<Location>> top3Routes = new ArrayList<>();
        for (int i = 0; i < 3 && i < routeScores.size(); i++) {
            top3Routes.add(routeScores.get(i).getKey());
        }

        return top3Routes;
    }

}
