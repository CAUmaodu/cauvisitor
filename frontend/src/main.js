import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import axios from 'axios'
import 'leaflet/dist/leaflet.css';

Vue.config.productionTip = false
Vue.use(ElementUI);

// 开发：可设 VUE_APP_API_BASE=http://localhost:8081/cauvisitor 直连后端（任意前端端口）
// 不设则使用相对路径 /cauvisitor，由 vue.config.js 的 devServer.proxy 转发到后端
const apiBase = (process.env.VUE_APP_API_BASE || '').trim();
axios.defaults.baseURL = apiBase ? apiBase.replace(/\/$/, '') : '/cauvisitor';

Vue.prototype.$axios = axios;

new Vue({
  render: h => h(App),
}).$mount('#app')