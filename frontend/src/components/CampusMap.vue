<template>
  <div class="container">
    <div id="map-container"></div>

    <div class="search-box">
      <el-input placeholder="请输入地点名称" v-model="searchKeyword" @keyup.enter.native="handleSearch" clearable>
        <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
      </el-input>
    </div>

    <div v-if="currentTool === 'pick-location'" class="tip-box">
      <i class="el-icon-location-information"></i> 请在地图上点击 <b>丢失</b> 或 <b>捡到</b> 物品的位置
      <el-button type="text" @click="clearTools" style="margin-left: 10px; color: #f56c6c;">取消</el-button>
    </div>
  </div>
</template>

<script>
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { POI_LAYER_PRESETS } from '../data/poi-presets.js';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

export default {
  name: 'CampusMap',
  data() {
    const layerGroups = {};
    POI_LAYER_PRESETS.forEach(p => { layerGroups[p.key] = new L.LayerGroup(); });
    return {
      map: null,
      searchKeyword: '',
      searchLayer: null,
      userLocationMarker: null,
      userLocation: null, // 保存用户定位的位置

      allPointsData: null,
      lostFoundData: null,

      layerGroups,
      lostFoundLayerGroup: new L.LayerGroup(),

      currentTool: null,
      measurePoints: [],
      measurePolyline: null,
      routeStart: null,
      routeEnd: null,
      routeLayer: null,

      currentBaseLayer: null,

      poiHighlightLayer: null
    };
  },
  mounted() {
    this.initMap();
    this.loadData();
    this.loadLostFoundData();
    setTimeout(() => { if (this.map) this.map.invalidateSize(); }, 200);
    setTimeout(() => { if (this.map) this.map.invalidateSize(); }, 500);
    setTimeout(() => { if (this.map) this.map.invalidateSize(); }, 1000);
  },
  methods: {
    apiPublicBase() {
      const b = (process.env.VUE_APP_API_BASE || '').trim();
      if (b && /^https?:\/\//i.test(b)) return b.replace(/\/$/, '');
      if (typeof window !== 'undefined') return `${window.location.origin}/cauvisitor`;
      return '/cauvisitor';
    },

    showPoiFeature(feature) {
      if (!this.map || !feature || !feature.geometry) return;
      if (this.poiHighlightLayer) {
        this.map.removeLayer(this.poiHighlightLayer);
        this.poiHighlightLayer = null;
      }
      const gj = this.convertGeoJSONToGCJ02(JSON.parse(JSON.stringify(feature)));
      const geom = gj.geometry;
      const name = (gj.properties && gj.properties.name) ? gj.properties.name : '兴趣点';
      if (geom.type === 'Point' && geom.coordinates && geom.coordinates.length >= 2) {
        const lng = geom.coordinates[0];
        const lat = geom.coordinates[1];
        const latlng = L.latLng(lat, lng);
        const marker = L.circleMarker(latlng, {
          radius: 12,
          color: '#E6A23C',
          weight: 3,
          fillColor: '#F56C6C',
          fillOpacity: 0.9
        });
        marker.bindPopup(`<strong>${name}</strong>`);
        this.poiHighlightLayer = L.layerGroup([marker]).addTo(this.map);
        marker.openPopup();
        this.map.setView(latlng, Math.max(this.map.getZoom(), 17));
        this.$message.success('已在地图上标出：' + name);
      } else {
        this.$message.warning('该要素不是点类型，无法在地图上高亮');
      }
    },

    initMap() {
      this.map = L.map('map-container', { attributionControl: false, zoomControl: false }).setView([40.004, 116.358], 17);
      this.switchBaseMap('day');

      this.map.on('click', this.handleMapClick);
      this.map.on('dblclick', this.finishMeasure);

      this.lostFoundLayerGroup.addTo(this.map);
    },

    loadData() {
      this.$axios.get('/pointmodel/all').then(res => {
        this.allPointsData = this.convertGeoJSONToGCJ02(res.data);
        POI_LAYER_PRESETS.filter(p => p.defaultOn).forEach(p => {
          this.updateLayer(p.key, true);
        });
      }).catch(e => console.error(e));
    },
    loadLostFoundData() {
      this.$axios.get('/lost/list').then(res => {
        this.lostFoundData = this.convertGeoJSONToGCJ02(res.data);
        this.renderLostFound();
      }).catch(e => console.error('加载失物招领失败', e));
    },

    renderLostFound() {
      this.lostFoundLayerGroup.clearLayers();
      if (!this.lostFoundData) return;
      L.geoJSON(this.lostFoundData, {
        onEachFeature: (feature, layer) => {
          const p = feature.properties;
          const titleColor = p.lostType === 'lost' ? '#F56C6C' : '#67C23A';
          const typeText = p.lostType === 'lost' ? '寻物启事' : '失物招领';
          const content = `<div style="width: 200px;"><h4 style="margin:0 0 5px 0; color:${titleColor}; border-bottom:1px solid #eee; padding-bottom:5px;">${typeText}: ${p.itemName}</h4><p style="margin:5px 0; color:#666; font-size:13px;">${p.description}</p><p style="margin:5px 0; font-size:12px;"><b>联系人:</b> ${p.visitorName}</p><p style="margin:5px 0; font-size:12px;"><b>联系方式:</b> ${p.contact}</p><p style="margin:5px 0; color:#999; font-size:12px;">${p.createTime}</p></div>`;
          layer.bindPopup(content);
        }
      }).addTo(this.lostFoundLayerGroup);
    },

    activateTool(toolType) {
      this.clearTools();
      this.currentTool = toolType;

      if (toolType === 'measure') this.$message.info('【距离测量】请单击选点，双击结束');
      else if (toolType === 'route') this.$message.info('【路径规划】请点击起点，再点击终点');
      else if (toolType === 'pick-location') this.$message.success('请在地图上点击 丢失/捡到 的位置');

      L.DomUtil.addClass(this.map._container, 'crosshair-cursor-enabled');
    },

    handleMapClick(e) {
      if (!this.currentTool) return;
      const latlng = e.latlng;

      if (this.currentTool === 'pick-location') {
        const wgs84 = this.gcj02towgs84(latlng.lng, latlng.lat);
        this.$emit('location-picked', { lat: wgs84[1], lng: wgs84[0] });
        this.clearTools();
        return;
      }

      if (this.currentTool === 'measure') {
        this.measurePoints.push(latlng);
        L.circleMarker(latlng, { radius: 4, color: 'red', fillColor: '#f03', fillOpacity: 1 }).addTo(this.map);
        if (this.measurePolyline) this.measurePolyline.setLatLngs(this.measurePoints);
        else this.measurePolyline = L.polyline(this.measurePoints, { color: 'red' }).addTo(this.map);
      } else if (this.currentTool === 'route') {
        if (!this.routeStart) {
          this.routeStart = L.marker(latlng).addTo(this.map).bindPopup('起点').openPopup();
          this.$message.success('起点已定，请选择终点');
        } else if (!this.routeEnd) {
          this.routeEnd = L.marker(latlng).addTo(this.map).bindPopup('终点').openPopup();
          this.calculateRealRoute();
          this.currentTool = null;
          L.DomUtil.removeClass(this.map._container, 'crosshair-cursor-enabled');
        }
      }
    },

    switchBaseMap(type) {
      if (!this.map) return;
      if (this.currentBaseLayer) this.map.removeLayer(this.currentBaseLayer);
      let newLayer = null;
      if (type === 'day') {
        newLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', { subdomains: ['1', '2', '3', '4'], minZoom: 1, maxZoom: 18 });
        this.$message.success('已切换至：标准地图');
      } else if (type === 'night') {
        newLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', { subdomains: ['1', '2', '3', '4'], minZoom: 1, maxZoom: 18, className: 'night-tiles' });
        this.$message.success('已切换至：夜间模式');
      } else if (type === 'satellite') {
        newLayer = L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', { subdomains: ['1', '2', '3', '4'], minZoom: 1, maxZoom: 18 });
        this.$message.success('已切换至：卫星影像');
      } else if (type === 'hybrid') {
        const satLayer = L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', { subdomains: ['1', '2', '3', '4'] });
        const labelLayer = L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=8&x={x}&y={y}&z={z}', { subdomains: ['1', '2', '3', '4'] });
        newLayer = L.layerGroup([satLayer, labelLayer]);
        this.$message.success('已切换至：混合路网');
      }
      if (newLayer) {
        newLayer.addTo(this.map);
        if (typeof newLayer.bringToBack === 'function') newLayer.bringToBack();
        this.currentBaseLayer = newLayer;
      }
    },
    updateLayer(type, isVisible) {
      if (!this.allPointsData) return;
      const preset = POI_LAYER_PRESETS.find(p => p.key === type);
      if (!preset) return;
      const group = this.layerGroups[type];
      if (!isVisible) {
        if (this.map.hasLayer(group)) this.map.removeLayer(group);
        return;
      }
      group.clearLayers();
      L.geoJSON(this.allPointsData, {
        filter: (feature) => {
          const name = feature.properties.name || '';
          return preset.keywords.some(kw => name.includes(kw));
        },
        onEachFeature: (feature, layer) => { layer.bindPopup(`<strong>${feature.properties.name}</strong>`); }
      }).eachLayer((layer) => group.addLayer(layer));
      this.map.addLayer(group);
    },
    calculateRealRoute() {
      const p1 = this.routeStart.getLatLng();
      const p2 = this.routeEnd.getLatLng();
      const loading = this.$loading({ lock: true, text: '正在通过高德规划步行路线...', spinner: 'el-icon-loading', background: 'rgba(0, 0, 0, 0.7)' });
      this.$axios.get('/route/plan', { params: { startLat: p1.lat, startLng: p1.lng, endLat: p2.lat, endLng: p2.lng } }).then(res => {
        loading.close();
        if (!res.data || !res.data.geometry) {
          this.$message.warning('无法规划路径：请确认高德 Key 已开通「Web 服务」中的路径规划，且控制台无 IP/Referer 限制冲突');
          return;
        }
        const isGcj = res.data.properties && res.data.properties.coordSys === 'GCJ02';
        const geoJsonForMap = isGcj ? res.data : this.convertGeoJSONToGCJ02(res.data);
        if (this.routeLayer) this.map.removeLayer(this.routeLayer);
        this.routeLayer = L.geoJSON(geoJsonForMap, { style: { color: '#409EFF', weight: 6, opacity: 0.88 } }).addTo(this.map);
        this.map.fitBounds(this.routeLayer.getBounds(), { padding: [50, 50] });
        const p = res.data.properties || {};
        let extra = '';
        if (p.distanceMeters != null) extra += ` 约 ${p.distanceMeters} 米`;
        if (p.durationSeconds != null) extra += ` / 约 ${Math.round(Number(p.durationSeconds) / 60)} 分钟`;
        this.$message.success('路径规划成功！' + extra);
      }).catch(err => { loading.close(); console.error(err); this.$message.error('路径规划请求失败'); });
    },
    finishMeasure() {
      if (this.currentTool !== 'measure') return;
      if (this.measurePoints.length < 2) return;
      let totalDistance = 0;
      for (let i = 0; i < this.measurePoints.length - 1; i++) totalDistance += this.map.distance(this.measurePoints[i], this.measurePoints[i + 1]);
      L.popup().setLatLng(this.measurePoints[this.measurePoints.length - 1]).setContent(`<b>总距离：${totalDistance.toFixed(2)} 米</b>`).openOn(this.map);
      this.currentTool = null;
      L.DomUtil.removeClass(this.map._container, 'crosshair-cursor-enabled');
    },
    clearTools() {
      this.measurePoints = [];
      if (this.measurePolyline) this.map.removeLayer(this.measurePolyline);
      this.measurePolyline = null;
      if (this.routeStart) this.map.removeLayer(this.routeStart);
      if (this.routeEnd) this.map.removeLayer(this.routeEnd);
      if (this.routeLayer) this.map.removeLayer(this.routeLayer);
      if (this.poiHighlightLayer) this.map.removeLayer(this.poiHighlightLayer);
      this.routeStart = null; this.routeEnd = null; this.routeLayer = null;
      this.poiHighlightLayer = null;
      this.currentTool = null;
      L.DomUtil.removeClass(this.map._container, 'crosshair-cursor-enabled');
      this.map.closePopup();
    },
    handleSearch() {
      if (!this.searchKeyword) { this.$message.warning('请输入关键词'); return; }
      this.$axios.get('/polygonmodel/search', { params: { name: this.searchKeyword } }).then(res => {
        const data = res.data;
        if (!data.features || data.features.length === 0) { this.$message.info('未找到相关地点'); return; }
        this.$message.success(`找到 ${data.features.length} 个结果`);
        if (this.searchLayer) this.map.removeLayer(this.searchLayer);
        const convertedData = this.convertGeoJSONToGCJ02(data);
        this.searchLayer = L.geoJSON(convertedData, {
          style: { color: 'red', fillColor: '#ff0000', fillOpacity: 0.6, weight: 3 },
          onEachFeature: (feature, layer) => {
            if (feature.properties && feature.properties.name) layer.bindPopup(`<strong>${feature.properties.name}</strong>`).openPopup();
          }
        }).addTo(this.map);
        this.map.fitBounds(this.searchLayer.getBounds(), { padding: [50, 50] });
      }).catch(err => { console.error(err); this.$message.error('搜索出错'); });
    },
    resetMap() { if (this.map) { this.map.setView([40.004, 116.358], 17); this.$message.success('地图视角已复位'); } },

    convertGeoJSONToGCJ02(geojson) {
      if (!geojson) return null;
      const data = JSON.parse(JSON.stringify(geojson));
      const processGeometry = (geometry) => {
        if (!geometry || !geometry.coordinates) return;
        if (geometry.type === 'Point') geometry.coordinates = this.wgs84togcj02(geometry.coordinates[0], geometry.coordinates[1]);
        else if (geometry.type === 'LineString' || geometry.type === 'MultiPoint') geometry.coordinates = geometry.coordinates.map(c => this.wgs84togcj02(c[0], c[1]));
        else if (geometry.type === 'Polygon' || geometry.type === 'MultiLineString') geometry.coordinates = geometry.coordinates.map(ring => ring.map(c => this.wgs84togcj02(c[0], c[1])));
        else if (geometry.type === 'MultiPolygon') geometry.coordinates = geometry.coordinates.map(poly => poly.map(ring => ring.map(c => this.wgs84togcj02(c[0], c[1]))));
      };
      if (data.type === 'FeatureCollection') data.features.forEach(f => processGeometry(f.geometry));
      else if (data.type === 'Feature') processGeometry(data.geometry);
      else processGeometry(data);
      return data;
    },
    wgs84togcj02(lng, lat) {
      if (this.out_of_china(lng, lat)) return [lng, lat];
      var dlat = this.transformlat(lng - 105.0, lat - 35.0); var dlng = this.transformlng(lng - 105.0, lat - 35.0);
      var radlat = lat / 180.0 * Math.PI; var magic = Math.sin(radlat);
      magic = 1 - 0.00669342162296594323 * magic * magic;
      var sqrtmagic = Math.sqrt(magic);
      dlat = (dlat * 180.0) / ((6378245.0 * (1 - 0.00669342162296594323)) / (magic * sqrtmagic) * Math.PI);
      dlng = (dlng * 180.0) / (6378245.0 / sqrtmagic * Math.cos(radlat) * Math.PI);
      return [lng + dlng, lat + dlat];
    },
    gcj02towgs84(lng, lat) {
      if (this.out_of_china(lng, lat)) return [lng, lat];
      var dlat = this.transformlat(lng - 105.0, lat - 35.0); var dlng = this.transformlng(lng - 105.0, lat - 35.0);
      var radlat = lat / 180.0 * Math.PI; var magic = Math.sin(radlat);
      magic = 1 - 0.00669342162296594323 * magic * magic;
      var sqrtmagic = Math.sqrt(magic);
      dlat = (dlat * 180.0) / ((6378245.0 * (1 - 0.00669342162296594323)) / (magic * sqrtmagic) * Math.PI);
      dlng = (dlng * 180.0) / (6378245.0 / sqrtmagic * Math.cos(radlat) * Math.PI);
      return [lng * 2 - (lng + dlng), lat * 2 - (lat + dlat)];
    },
    transformlat(lng, lat) {
      var ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
      ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
      ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0;
      ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0;
      return ret;
    },
    transformlng(lng, lat) {
      var ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
      ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
      ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0;
      ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0;
      return ret;
    },
    out_of_china(lng, lat) { return (lng < 72.004 || lng > 137.8347) || ((lat < 0.8293 || lat > 55.8271)); },

    locateUser(latitude, longitude) {
      this.$message.success('定位成功！');
      
      // 保存用户位置（WGS84坐标）
      this.userLocation = { lat: latitude, lng: longitude };
      
      const gcj02 = this.wgs84togcj02(longitude, latitude);
      const latlng = L.latLng(gcj02[1], gcj02[0]);
      
      if (this.userLocationMarker) {
        this.map.removeLayer(this.userLocationMarker);
      }
      
      this.userLocationMarker = L.marker(latlng, {
        icon: L.divIcon({
          html: '<div style="background: #4CAF50; width: 20px; height: 20px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.4);"></div>',
          iconSize: [20, 20],
          iconAnchor: [10, 10]
        })
      }).addTo(this.map);
      
      this.map.setView(latlng, 18);
    },

    navigateToPoi(feature) {
      // 首先显示POI
      this.showPoiFeature(feature);
      
      // 检查是否有用户位置
      if (!this.userLocation) {
        this.$message.warning('请先点击「定位我的位置」获取您的位置，然后再进行导航');
        return;
      }
      
      // 获取POI的位置
      if (!feature.geometry || !feature.geometry.coordinates) {
        this.$message.warning('无法获取POI位置');
        return;
      }
      
      const poiLng = feature.geometry.coordinates[0];
      const poiLat = feature.geometry.coordinates[1];
      
      // 开始路径规划
      this.clearTools();
      this.currentTool = 'route';
      
      // 设置起点为用户位置
      const userGcj02 = this.wgs84togcj02(this.userLocation.lng, this.userLocation.lat);
      this.routeStart = L.marker(L.latLng(userGcj02[1], userGcj02[0])).addTo(this.map).bindPopup('我的位置').openPopup();
      
      // 设置终点为POI位置
      const poiGcj02 = this.wgs84togcj02(poiLng, poiLat);
      this.routeEnd = L.marker(L.latLng(poiGcj02[1], poiGcj02[0])).addTo(this.map).bindPopup('目的地');
      
      // 计算路线
      this.calculateRealRoute();
    },


  }
};
</script>

<style scoped>
.container >>> .crosshair-cursor-enabled { cursor: crosshair; }
.container { position: relative; width: 100%; height: 100%; padding: 0; margin: 0; }
#map-container { width: 100%; height: 100%; z-index: 1; background-color: #ddd; padding: 0; margin: 0; }
.search-box { position: absolute; top: 20px; left: 50px; z-index: 2000; width: 300px; background: white; border-radius: 4px; box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); }
.tip-box { position: absolute; top: 80px; left: 50%; transform: translateX(-50%); z-index: 2000; background: rgba(255, 255, 255, 0.9); padding: 10px 20px; border-radius: 4px; box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1); font-size: 14px; color: #333; }
.container >>> .night-tiles {
  filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%) grayscale(20%);
  -webkit-filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%) grayscale(20%);
}
</style>
