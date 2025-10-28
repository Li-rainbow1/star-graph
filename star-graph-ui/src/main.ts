import { createApp } from 'vue'
import App from './App.vue'
import router from "@/router";
import { setupElIcons } from "@/plugins";
import ElementPlus from "element-plus"
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persist'

import 'element-plus/dist/index.css'
import "@/styles/index.scss";
import "animate.css";
const pinia =createPinia();
pinia.use(piniaPersist);

const app = createApp(App);
// 全局注册Element-plus图标
setupElIcons(app);
app.use(router).use(pinia).use(ElementPlus).mount("#app");