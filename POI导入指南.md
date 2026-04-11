# CAU校园地图 - POI地标导入指南

## 一、数据库表结构

### 1.1 cau_point 表（兴趣点表）
| 字段 | 类型 | 说明 | 示例值 |
|------|------|------|--------|
| gid | integer | 主键（自增） | 1, 2, 3... |
| fid | numeric | 外部ID（可选） | 100, 101... |
| name | varchar(255) | 地标名称 | '图书馆', '食堂' |
| geom | geometry(Point,4326) | 经纬度坐标 | 见下方说明 |

**注意**：`geom` 使用 WGS84 坐标系 (SRID 4326)

---

## 二、如何获取经纬度坐标

### 方法1：使用高德地图/百度地图拾取坐标
1. 访问 [高德地图坐标拾取器](https://lbs.amap.com/tools/picker)
2. 在地图上点击位置，获取经纬度
3. 例如：`116.3580, 40.0045`

### 方法2：使用在线地图工具
- [百度地图坐标拾取](https://api.map.baidu.com/lbsapi/getpoint/index.html)
- [天地图坐标拾取](http://lbs.tianditu.gov.cn/jsapi/examples/coord_picker.html)

---

## 三、如何插入地标数据

### 3.1 方式一：使用 SQL 语句（推荐）

连接数据库后执行：

```sql
-- 插入单个地标
INSERT INTO public.cau_point (fid, name, geom)
VALUES (
    200,  -- fid（可选，随便填）
    '中国农业大学图书馆',  -- 名称
    ST_GeomFromText('POINT(116.3580 40.0045)', 4326)  -- 经纬度
);

-- 插入多个地标
INSERT INTO public.cau_point (fid, name, geom) VALUES
(201, '东门', ST_GeomFromText('POINT(116.3590 40.0050)', 4326)),
(202, '食堂', ST_GeomFromText('POINT(116.3575 40.0038)', 4326)),
(203, '操场', ST_GeomFromText('POINT(116.3570 40.0042)', 4326));
```

### 3.2 方式二：使用 pgAdmin 图形界面
1. 打开 pgAdmin，连接到 `cau_db`
2. 展开 `Schemas` → `public` → `Tables`
3. 右键 `cau_point` → `View/Edit Data` → `All Rows`
4. 点击添加行按钮（+）
5. 填写 `fid`、`name`
6. 对于 `geom` 列：
   - 双击单元格
   - 输入：`POINT(经度 纬度)`
   - 或使用 PostGIS 函数：`ST_GeomFromText('POINT(116.358 40.004)', 4326)`
7. 保存（F6）

---

## 四、常见地标坐标示例（农大附近）

| 地标名称 | 经度 | 纬度 |
|---------|------|------|
| 中国农业大学（东校区） | 116.3580 | 40.0045 |
| 中国农业大学图书馆 | 116.3583 | 40.0032 |
| 六道口地铁站 | 116.3550 | 40.0060 |
| 星巴克（六道口店） | 116.3560 | 40.0055 |
| 中国人民大学继续教育学院 | 116.3600 | 40.0020 |

---

## 五、验证数据是否导入成功

```sql
-- 查询所有地标
SELECT gid, name, ST_AsText(geom) as wkt, ST_X(geom) as lng, ST_Y(geom) as lat
FROM public.cau_point;

-- 按名称查询
SELECT * FROM public.cau_point WHERE name LIKE '%图书馆%';
```

---

## 六、坐标格式说明

### WKT 格式（Well-Known Text）
```
POINT(经度 纬度)
```
例如：`POINT(116.358 40.0045)`

### PostGIS 函数格式
```sql
ST_GeomFromText('POINT(116.358 40.0045)', 4326)
```

---

## 七、删除地标数据

```sql
-- 删除单个地标
DELETE FROM public.cau_point WHERE gid = 1;

-- 按名称删除
DELETE FROM public.cau_point WHERE name = '测试地标';

-- 清空所有（谨慎！）
DELETE FROM public.cau_point;
```

---

## 八、完整示例 SQL

```sql
-- 1. 插入农大图书馆
INSERT INTO public.cau_point (fid, name, geom)
VALUES (200, '中国农业大学图书馆', ST_GeomFromText('POINT(116.3583 40.0032)', 4326));

-- 2. 插入星巴克
INSERT INTO public.cau_point (fid, name, geom)
VALUES (201, '星巴克', ST_GeomFromText('POINT(116.3560 40.0055)', 4326));

-- 3. 插入人大继续教育学院
INSERT INTO public.cau_point (fid, name, geom)
VALUES (202, '中国人民大学继续教育学院', ST_GeomFromText('POINT(116.3600 40.0020)', 4326));

-- 4. 插入更多地标...
INSERT INTO public.cau_point (fid, name, geom) VALUES
(203, '东门', ST_GeomFromText('POINT(116.3590 40.0050)', 4326)),
(204, '第一教学楼', ST_GeomFromText('POINT(116.3578 40.0040)', 4326)),
(205, '第二教学楼', ST_GeomFromText('POINT(116.3585 40.0038)', 4326)),
(206, '学生食堂', ST_GeomFromText('POINT(116.3570 40.0035)', 4326)),
(207, '体育馆', ST_GeomFromText('POINT(116.3565 40.0048)', 4326)),
(208, '校医院', ST_GeomFromText('POINT(116.3595 40.0030)', 4326));
```
