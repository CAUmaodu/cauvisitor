package com.campus.vistorservice.controller;

import com.campus.vistorservice.dao.SuggestionMapper;
import com.campus.vistorservice.model.Suggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/suggestion")
public class SuggestionController {

    @Autowired
    private SuggestionMapper suggestionMapper;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addSuggestion(@RequestBody Suggestion suggestion) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (suggestion.getContent() == null || suggestion.getContent().trim().isEmpty()) {
            body.put("ok", false);
            body.put("msg", "请填写建议内容");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        if (suggestion.getVisitorName() == null || suggestion.getVisitorName().isEmpty()) {
            suggestion.setVisitorName("游客");
        }
        try {
            suggestionMapper.insert(suggestion);
            body.put("ok", true);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            body.put("ok", false);
            body.put("msg", "写入失败：请确认已创建 sys_suggestion 表（执行 sql/schema.sql）");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<Suggestion>> getSuggestionList() {
        return ResponseEntity.ok(suggestionMapper.findAll());
    }
}
