import vue from '@vitejs/plugin-vue'
import { resolve } from "path";
import { UserConfig, ConfigEnv, loadEnv, defineConfig } from "vite";

const pathSrc = resolve(__dirname, "./src");

// https://vitejs.dev/config/
export default defineConfig(({ mode }: ConfigEnv): UserConfig => {
  const env = loadEnv(mode, process.cwd());
  return {
    resolve: {
      alias: {
        "@": pathSrc,
      },
    },
    css: {
      // CSS 预处理器
      preprocessorOptions: {
        // 定义全局 SCSS 变量
        scss: {
          javascriptEnabled: true,
          additionalData: `
            @use "@/styles/variables.scss" as *;
          `,
        },
      },
    },
    server: {
      // 允许IP访问
      host: "0.0.0.0",
      // 应用端口 (默认:3000)
      port: Number(env.VITE_APP_PORT),
      // 运行是否自动打开浏览器
      open: true,
      proxy: {
        /** 代理前缀为 /dev-api 的请求  */
        [env.VITE_PREFIX_BASE_API]: {
          changeOrigin: true,
          // 接口地址
          target: env.VITE_PROXY_URL,
          rewrite: (path) =>
              path.replace(new RegExp("^" + env.VITE_PREFIX_BASE_API), ""),
        },
      },
    },
    plugins: [
      vue()
    ],
    define: {
      __APP_ENV__: JSON.stringify(env.APP_ENV),
    },
  }
})
