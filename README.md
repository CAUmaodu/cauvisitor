# 智慧校园导览系统

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Docker](https://img.shields.io/badge/docker-compose-green.svg)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue.js-3.x-4FC08D.svg)
![PostGIS](https://img.shields.io/badge/PostGIS-3.5-blue.svg)

## 📖 项目简介 (Introduction)

这是一个基于 **WebGIS** 技术的 **智慧校园导览系统 (Smart Campus Visitor System)**。它专为校园场景设计，集成了校园高精度地图展示、POI 兴趣点查询、**校内路径规划 (最短路径导航)** 、3D街景以及失物招领等功能，致力于打造便捷的**智慧游客系统**。


### ✨ 核心功能
* 🗺️ **校园地图**：基于 Leaflet/Mapbox 的高精度校园底图展示。
* 🛣️ **路径规划**：支持校内任意两点间的**最短路径计算** (基于 高德api)。
* 📍 **POI 查询**：教学楼、宿舍、食堂等兴趣点的快速检索与定位。
* 📢 **失物招领**：互动的失物招领信息发布与查询平台。
* 🚌 **3D街景**：校园内部3D街景的可视化展示。

---

## 🛠️ 技术栈 (Tech Stack)

* **前端**：Vue.js, Element UI, Leaflet / Mapbox GL
* **后端**：Java Spring Boot, MyBatis
* **数据库**：PostgreSQL 16
    * ➕ **PostGIS** (处理地理空间几何数据)
    * ➕ **pgRouting** (核心：处理路径规划算法)

---
在线访问网站： http://101.200.121.83/  点击即可访问
💻部署到服务器：

1. 安装运行环境：JDK 17、Node.js、PostgreSQL 14、IIS

2. 安装IIS扩展：URL Rewrite模块、ARR(Application Request Routing)

3. 启用ARR代理功能：IIS管理器→服务器节点→Application Request Routing Cache→Server Proxy Settings→勾选Enable proxy

4. 导入数据库：psql执行schema.sql建表 data.sql导入初始数据

5. 构建前端：cd frontend && npm run build 生成dist目录

6. 配置IIS网站：物理路径指向dist目录 端口80

7. 部署web.config：在dist目录放入URL Rewrite规则（反向代理/cauvisitor至localhost:8081）

8. 启动后端：java -jar VistorService.jar 或 mvn spring-boot:run

9. 开放安全组：阿里云控制台入方向放通TCP 80端口本地防火墙：PowerShell添加80端口入站规则
💻 本地源码启动 (适合开发者)
如果你想修改代码，可以在本地手动搭建环境运行。

1. 前置准备 (Prerequisites)
你需要在电脑上安装以下软件：

Java JDK 17+

Node.js 16+ (建议使用 LTS 版本)

PostgreSQL 16 (数据库)

PostGIS 插件 (必须安装)

pgRouting 插件 (必须安装，这是最容易报错的一步！)

2. 数据库配置 (最关键的一步)
安装插件：在安装 PostgreSQL 后，请务必使用 Application Stack Builder 安装 PostGIS 扩展（通常 pgRouting 会包含在 PostGIS 的安装包选项中，或者需要单独下载）。

注意：Windows 用户安装 pgRouting 可能比较麻烦，务必确保 CREATE EXTENSION pgrouting; 能在你的本地数据库执行成功。

创建数据库：新建一个名为 cau_db 的数据库。

导入数据：

先运行 sql/schema.sql (建立表结构)。

再运行 sql/data.sql (导入数据)。

3. 后端启动 (Spring Boot)
进入后端目录：cd cauvisitor-backend

修改配置：打开 src/main/resources/application.yml，修改数据库连接信息：

spring:
  datasource:
    # 如果是本地启动，这里要填 localhost，而不是 docker 里的 db
    url: jdbc:postgresql://localhost:5432/cau_db
    username: postgres
    password: 你的数据库密码

启动项目：

命令行：mvn spring-boot:run

IDEA：直接找到 VisitorServiceApplication.java 右键 Run。

4. 前端启动 (Vue)
进入前端目录：cd cauvisitor-frontend

安装依赖：
npm install
# 或者使用淘宝镜像
npm install --registry=[https://registry.npmmirror.com](https://registry.npmmirror.com)

启动开发服务器：
npm run dev
# 或者
npm run serve

访问：http://localhost:8082 (端口视具体配置而定)


📂 目录结构说明

cauvisitor
├── sql/                 # 数据库脚本
│   ├── schema.sql       # 🏗️ 表结构定义 (含 PostGIS/pgRouting 插件开启)
│   └── data.sql         # 💾 初始演示数据 (POI、路网数据)
├── backend/   # 后端 Java 源码
├── frontend/  # 前端 Vue 源码
└── README.md            # 项目说明文档

💡 混合开发建议 (强烈推荐)
鉴于我们在解决 pgRouting 插件时遇到的巨大困难，强烈建议开发时采用高德webapi：

代码在本地跑：在 IDEA 里运行 Java，在 VS Code 里运行 Vue。

连接方式：把本地后端代码的数据库连接指向 localhost:5433 。

❓ 常见问题 (Troubleshooting)
Q1: 启动后访问 http://localhost 是一片空白或报错？
原因：后端服务可能还没完全启动，或者数据库正在导入大量数据。

Q2: 报错 "Port 5432 is already allocated"？
原因：你本地电脑上已经安装并运行了 PostgreSQL，占用了 5432 端口。

解决：

方法A：停止本地的 PostgreSQL 服务。

👨‍💻 开发者指南
如果你想修改代码：

修改前端：在 cauvisitor-frontend 目录下开发，修改后需重新构建镜像或挂载目录。

修改后端：在 cauvisitor-backend 目录下开发 (JDK 17+)。

修改数据库：使用qgis将新的 SQL 语句更新到 sql/ 目录中。
