package com.campus.vistorservice.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用高德「步行路径规划」REST API（坐标为 GCJ-02，与前端高德底图一致）。
 */
@RestController
@RequestMapping("/route")
public class RouteController {

    private static final String AMAP_WALK_URL = "https://restapi.amap.com/v3/direction/walking";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${amap.key:}")
    private String amapKey;

    @GetMapping("/plan")
    public JSONObject planRoute(@RequestParam double startLat, @RequestParam double startLng,
                                @RequestParam double endLat, @RequestParam double endLng) {
        if (!isAmapKeyConfigured()) {
            System.err.println("路径规划：请在 application.yml 或环境变量 AMAP_KEY 中配置有效的高德 Web 服务 Key");
            return null;
        }
        try {
            String url = UriComponentsBuilder.fromUriString(AMAP_WALK_URL)
                    .queryParam("key", amapKey)
                    .queryParam("output", "json")
                    .queryParam("origin", startLng + "," + startLat)
                    .queryParam("destination", endLng + "," + endLat)
                    .toUriString();

            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isEmpty()) {
                return null;
            }
            JSONObject root = JSON.parseObject(body);
            if (!"1".equals(root.getString("status"))) {
                System.err.println("高德路径规划失败: " + root.getString("info"));
                return null;
            }
            JSONObject routeObj = root.getJSONObject("route");
            if (routeObj == null) {
                return null;
            }
            JSONArray paths = routeObj.getJSONArray("paths");
            if (paths == null || paths.isEmpty()) {
                return null;
            }
            JSONObject path0 = paths.getJSONObject(0);
            JSONArray steps = path0.getJSONArray("steps");
            if (steps == null || steps.isEmpty()) {
                return null;
            }

            List<JSONArray> coordPairs = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                String polyline = steps.getJSONObject(i).getString("polyline");
                if (polyline == null || polyline.isEmpty()) {
                    continue;
                }
                for (String seg : polyline.split(";")) {
                    String[] ll = seg.split(",");
                    if (ll.length >= 2) {
                        double lng = Double.parseDouble(ll[0].trim());
                        double lat = Double.parseDouble(ll[1].trim());
                        JSONArray pair = new JSONArray();
                        pair.add(lng);
                        pair.add(lat);
                        coordPairs.add(pair);
                    }
                }
            }
            if (coordPairs.isEmpty()) {
                return null;
            }

            JSONObject geometry = new JSONObject();
            geometry.put("type", "LineString");
            geometry.put("coordinates", coordPairs);

            JSONObject properties = new JSONObject();
            properties.put("coordSys", "GCJ02");
            if (path0.get("distance") != null) {
                properties.put("distanceMeters", path0.get("distance"));
            }
            if (path0.get("duration") != null) {
                properties.put("durationSeconds", path0.get("duration"));
            }

            JSONObject feature = new JSONObject();
            feature.put("type", "Feature");
            feature.put("properties", properties);
            feature.put("geometry", geometry);
            return feature;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isAmapKeyConfigured() {
        if (amapKey == null || amapKey.isBlank()) {
            return false;
        }
        return !amapKey.contains("请替换");
    }
}
