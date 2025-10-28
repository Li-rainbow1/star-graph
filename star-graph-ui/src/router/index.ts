import { createRouter, createWebHashHistory, RouteRecordRaw } from "vue-router";

// 静态路由
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/home",
  },
  {
    path: "/home",
    name: "Home",
    component: () => import("@/views/home/index.vue"),
    meta: {
      title: "首页",
    },
  },
  {
    path: "/t2i",
    name: "Text2Image",
    component: () => import("@/views/t2i/index.vue"),
    meta: {
      title: "文生图",
    },
  },
  {
    path: "/i2i",
    name: "Image2Image",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "图生图",
    },
  },
  {
    path: "/i24k",
    name: "Image24k",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "画质提升",
    },
  },
  {
    path: "/z2i",
    name: "Pose2Image",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "姿势生图",
    },
  },
  {
    path: "/t2v",
    name: "Text2Video",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "文生视频",
    },
  },
  {
    path: "/i2v",
    name: "Image2Video",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "图生视频",
    },
  },
  {
    path: "/fggc",
    name: "fggc",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "风格广场",
    },
  },
  {
    path: "/fgxl",
    name: "fgxl",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "风格训练",
    },
  },
  {
    path: "/wdtp",
    name: "wdtp",
    component: () => import("@/views/userImage/index.vue"),
    meta: {
      title: "我的图片",
    },
  },
  {
    path: "/wdsc",
    name: "wdsc",
    component: () => import("@/views/empty.vue"),
    meta: {
      title: "我的收藏",
    },
  },
];

/**
 * 创建路由
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

export default router;
