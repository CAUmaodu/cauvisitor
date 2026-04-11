package com.campus.vistorservice.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.campus.vistorservice.dao.LostFoundMapper;
import com.campus.vistorservice.model.LostFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lost")
public class LostFoundController {

    @Autowired
    private LostFoundMapper lostFoundMapper;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody LostFound lostFound) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (lostFound.getLongitude() == null || lostFound.getLatitude() == null) {
            body.put("ok", false);
            body.put("msg", "请先在地图上点击拾取丢失/捡到位置");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        if (lostFound.getVisitorName() == null || lostFound.getVisitorName().isEmpty()) {
            lostFound.setVisitorName("游客");
        }
        try {
            lostFoundMapper.insert(lostFound);
            body.put("ok", true);
            body.put("msg", "发布成功");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            body.put("ok", false);
            body.put("msg", "写入失败：请确认数据库已执行 sql/schema.sql、已安装 PostGIS，且 sys_lost_found 表存在");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/list")
    public JSONObject list() {
        List<LostFound> list = lostFoundMapper.findAllActive();

        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");
        List<JSONObject> features = new ArrayList<>();

        for (LostFound item : list) {
            JSONObject feature = new JSONObject();
            feature.put("type", "Feature");

            JSONObject properties = new JSONObject();
            properties.put("id", item.getId());
            properties.put("itemName", item.getItemName());
            properties.put("description", item.getDescription());
            properties.put("contact", item.getContact());
            properties.put("lostType", item.getLostType());
            properties.put("createTime", item.getCreateTime() != null ? item.getCreateTime().toString() : "");
            properties.put("visitorName", item.getVisitorName());
            feature.put("properties", properties);

            if (item.getGeometry() != null) {
                feature.put("geometry", JSON.parse(item.getGeometry()));
            }
            features.add(feature);
        }

        featureCollection.put("features", features);
        return featureCollection;
    }

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestParam Integer id) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            lostFoundMapper.deleteById(id);
            body.put("ok", true);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            body.put("ok", false);
            body.put("msg", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
