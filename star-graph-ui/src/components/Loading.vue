<template>
  <div class="load-mask" v-if="show">
    <div class="load-body">
      <div class="load-icon">
        <video src="../assets/images/loading.mp4" autoplay loop muted width="120" style="border-radius: 6px" ></video>
      </div>
      <div class="load-text">
        <div class="text">请稍后，您的图片正在生成中...</div>
        <el-progress
            class="lprogress"
            :text-inside="true"
            :stroke-width="15"
            :percentage="progress"
            status="exception"
        />
        <div class="queue" v-if="currentQueueIndex>0">在您前面有 <label>{{queueIndex-currentQueueIndex}}</label> 人在排队，请耐心等待</div>
        <div class="queue" v-else-if="started">您的图片生成正在生成，请耐心等待</div>
        <div class="queue" v-else>在您的任务序号是 <label>{{queueIndex}}</label> ，请耐心等待</div>
        <div class="button" v-if="!started">
          <div class="but-blue" style="background-color: #888888" @click="canelGen">取消生成</div>
          <div class="but-blue" @click="proprityTask">插队生成</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";

defineProps({
  proprityTask:{
    type: Function,
    required: true
  },
  canelGen:{
    type: Function,
    required: true
  },
  currentQueueIndex: {
    type: Number,
    default: 0,
    required: true
  },
  queueIndex: {
    type: Number,
    default: 0,
    required: true
  }
});


const show = ref(false)
const progress = ref(0)
const started = ref(false)

function openLoading() {
  started.value = false
  progress.value = 0
  show.value = true
}
function closeLoading() {
  show.value = false
  started.value = false
}
function startTask() {
  started.value = true
}
function updateProgress(num) {
  progress.value = parseInt(num)
  if(num==100){
    closeLoading();
  }
}
defineExpose({ openLoading,closeLoading,updateProgress,startTask});

</script>

<style scoped>
.load-mask {
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
  .load-body {
    display: flex;
    width: 300px;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border-radius: 6px;
    background-color: #FFFFFF;
    padding: 15px 20px 15px;
    .lprogress {
      width: 100%;
      height: 20px;
    }
  }
  .load-text {
    display: flex;
    flex-direction: column;
    align-content: center;
    justify-content: center;
    text-align: center;
    color: #4e4afc;
    font-size: 13px;
    .button {
      display: flex;
      margin: 10px;
    }
    .queue {
      color: #666666;
      font-size: 12px;
      label {
        color: red;
      }
    }
  }
}
</style>