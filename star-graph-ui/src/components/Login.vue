<template>
  <div class="login-mask" v-if="show">
    <div class="login-body">
      <div class="login-icon"></div>
      <div class="login-right">
        <div class="login-close" @click="show=false"></div>
        <div class="login-area">
          <div class="l-title">用户登录</div>
          <div class="fp-body"><label>+86</label><input class="input" v-model="form.username" placeholder="请输入用户名"/></div>
          <div class="fp-body"><label>密码</label><input class="input" v-model="form.password" type="password" placeholder="请输入8-20位密码"/></div>
          <a class="l-forget">忘记密码？</a>
          <div class="l-button" @click="handleLogin">立即登录</div>
          <div class="l-register">还没有账号？<a>立即注册</a></div>
          <div class="l-agreement">登录或完成注册即视为同意《<a>用户服务协议</a>》和《<a>隐私政策</a>》</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";
import {ElMessage} from "element-plus";
import UserAPI from "@/api/user";
import {useUserStore} from "@/store";
const show = ref(false)

const userStore = useUserStore();
const form = ref({
  username: 'admin',
  password: 'admin'
})

function openLogin() {
  show.value = true
}

function handleLogin(){
  if(form.value.username && form.value.password){
    UserAPI.login(form.value).then(res => {
      localStorage.setItem('accessToken',res.token)
      userStore.login(res)
      show.value = false
    });
  }else{
    ElMessage.error('请输入用户名或密码')
  }
}

defineExpose({ openLogin });

</script>

<style scoped>
.login-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0,0,0,0.2);
  z-index: 999;
  display: flex;
  justify-content: center;
  align-items: center;
  .login-body {
    display: flex;
    border-radius: 5px;
    .login-right{
      width: 350px;
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      .login-area {
        background-color: #FFFFFF;
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-content: center;
        padding: 30px 30px 0px;
        border-top-right-radius: 5px;
        border-bottom-right-radius: 5px;
        .l-title{
          font-family: PingFangSC-SNaNpxibold;
          font-weight: 600;
          font-size: 32px;
          color: #4E4AFC;
          letter-spacing: 0;
          text-align: center;
          margin-bottom: 10px;
        }
        .l-forget {
          font-size: 12px;
          color: #888888;
          text-align: right;
          margin-top: 10px;
        }
        .l-button {
          width: 100%;
          height: 32px;
          background: #FF7600;
          line-height: 32px;
          border-radius: 6px;
          text-align: center;
          font-family: PingFangSC-Regular;
          font-weight: 400;
          font-size: 14px;
          color: #FFFFFF;
          cursor: pointer;
          margin-top: 6px;
        }
        .l-register {
          font-size: 12px;
          color: #888888;
          text-align: center;
          margin-top: 18px;
          a {
            color: #4e4afc;
          }
        }
        .l-agreement {
          font-size: 10px;
          color: #888888;
          text-align: center;
          margin-top: 30px;
          a {
            color: #4e4afc;
          }
        }
      }
      .login-close {
        background: url("../assets/images/Close.png");
        background-size: 100% 100%;
        width: 16px;
        height: 18px;
        margin: 6px 0px 5px;
        cursor: pointer;
      }
    }
    .login-icon {
      background: url("../assets/images/img_denglu.png");
      background-size: 100% 100%;
      width: 305px;
      height: 363px;
    }
  }
}
</style>