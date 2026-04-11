package com.campus.vistorservice.model;

public class Suggestion {
    private Integer id;
    private String visitorName;
    private String content;

    /** 由 SQL to_char 映射为字符串，避免 Jackson / JDBC 时间类型序列化 500 */
    private String createTime;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}