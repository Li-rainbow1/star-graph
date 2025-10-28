<template>
  <div class="header">
    <div class="header-box">
      <div class="logo"></div>
      <div class="tool">
        <div class="slg"></div>
        <el-popover
            v-if="userStore.user.isLogin"
            :width="200"
            popper-style="box-shadow: rgb(14 18 22 / 35%) 0px 10px 38px -10px, rgb(14 18 22 / 20%) 0px 10px 20px -15px; padding: 20px;"
        >
          <template #reference>
            <el-avatar :src="userStore.user.avatar" style="margin: -5px 20px 0px 5px" />
          </template>
          <template #default>
            <div class="userinfo">
            <el-menu
                default-active="1"
            >
              <el-menu-item index="1">
                <template #title>
                  <el-avatar :src="userStore.user.avatar" style="margin: 0px 10px" size="small"/>
                  <span>{{ userStore.user.userName }}</span>
                </template>
              </el-menu-item>
            </el-menu>
            <el-button text icon="switchButton" @click="logout">Logout</el-button>
            </div>
          </template>
        </el-popover>
        <div class="but-blue" v-else="userStore.user.isLogin" @click="showLogin">登录/注册</div>
      </div>
    </div>
  </div>
  <div class="content">
    <div class="menu">
      <div :class="['mitem', path === '/home' ? 'active' : '']" @click="go('/home')">
        <div class="icon icon-home"></div>
        <div class="text">首页</div>
      </div>
      <div class="msplit">
        <div class="text">图像生成</div>
      </div>
      <div :class="['mitem', path === '/t2i' ? 'active' : '']" @click="go('/t2i')">
        <div class="icon icon-t2i"></div>
        <div class="text">文生图</div>
      </div>
      <div :class="['mitem', path === '/i2i' ? 'active' : '']" @click="go('/i2i')">
        <div class="icon icon-i2i"></div>
        <div class="text">图生图</div>
      </div>
      <div :class="['mitem', path === '/i24k' ? 'active' : '']" @click="go('/i24k')">
        <div class="icon icon-i24k"></div>
        <div class="text">画质提升</div>
      </div>
      <div :class="['mitem', path === '/z2i' ? 'active' : '']" @click="go('/z2i')">
        <div class="icon icon-z2i"></div>
        <div class="text">姿势生图</div>
      </div>
      <div class="msplit">
        <div class="text">视频生成</div>
      </div>
      <div :class="['mitem', path === '/t2v' ? 'active' : '']" @click="go('/t2v')">
        <div class="icon icon-t2v"></div>
        <div class="text">文生视频</div>
      </div>
      <div :class="['mitem', path === '/i2v' ? 'active' : '']" @click="go('/i2v')">
        <div class="icon icon-i2v"></div>
        <div class="text">图生视频</div>
      </div>
      <div class="msplit">
        <div class="text">模型训练</div>
      </div>
      <div :class="['mitem', path === '/fggc' ? 'active' : '']" @click="go('/fggc')">
        <div class="icon icon-fggc"></div>
        <div class="text">风格广场</div>
      </div>
      <div :class="['mitem', path === '/fgxl' ? 'active' : '']" @click="go('/fgxl')">
        <div class="icon icon-fgxl"></div>
        <div class="text">风格训练</div>
      </div>
      <div class="msplit">
        <div class="text">我的资产</div>
      </div>
      <div :class="['mitem', path === '/wdtp' ? 'active' : '']" @click="go('/wdtp')">
        <div class="icon icon-wdfg"></div>
        <div class="text">我的图片</div>
      </div>
      <div :class="['mitem', path === '/wdsc' ? 'active' : '']" @click="go('/wdsc')">
        <div class="icon icon-wdsc"></div>
        <div class="text">我的收藏</div>
      </div>
    </div>
    <div class="body"><router-view /></div>
  </div>
  <login ref="login" />
</template>
<script setup lang="ts">
import {useRouter} from "vue-router";
import {ref} from "vue";
import Login from "@/components/Login.vue";
import {useUserStore} from "@/store";
import {ElMessageBox} from "element-plus";

const router = useRouter();
const userStore = useUserStore();

const path = ref();
path.value = router.currentRoute.value
function go(link: string){
  router.push(link)
  path.value = link
}

const login = ref<Login>();
function showLogin(){
  login.value.openLogin()
}

function logout(){
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
  })
}

</script>

<style scoped>
</style>
