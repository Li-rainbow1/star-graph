<template>
  <div class="riv">
    <div class="riv-body">
      <div class="iview">
        <img :src="images[active]">
      </div>
      <div class="t2itool">
        <img src="../assets/icons/hb.png" alt="局部修改" height="27" width="27"/>
        <img src="../assets/icons/2k.png" alt="转成高清2K" height="27" width="27"/>
        <img src="../assets/icons/4k.png" alt="转成高清4K" height="27" width="27"/>
        <img src="../assets/icons/xz.png" alt="下载" height="27" width="27"/>
      </div>
    </div>
    <div class="iview-list">
      <img v-for="(item,index) in images" :src="item" @click="onSelected(index)" :class="{active: index === active}">
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";

defineProps({
  images: {
    type: Array,
    required: true
  }
});
const active = ref(0);

function onSelected(index){
  active.value = index;
}
defineExpose({onSelected})
</script>

<style scoped>
.riv {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  .riv-body {
    display: flex;
    flex-direction: row;
    .iview {
      box-shadow: 0 0 28px #00000005;
      border-radius: 10px;
      min-width: 512px;
      min-height: 512px;
      max-width: 788px;
      max-height: 788px;
      background: url("../assets/images/pic-tp.png") no-repeat #F5F5F5 center center;
      padding: 6px 6px 0px;
      img{
        margin: 0px;
        padding: 0px;
        border-radius: 5px;
      }
    }
    .t2itool {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-left: 5px;
      img{
        padding: 5px;
        border-radius: 10px;
        cursor: pointer;
      }
      img:hover {
        background-color: #F5F5F5;
      }
    }
  }
  .iview-list{
    display: flex;
    width: 516px;
    margin-right: 30px;
    margin-top: 15px;
    overflow-x: auto;
    padding: 0px 15px 10px 0px;
    img {
      border-radius: 5px;
      max-width: 118px;
      max-height: 118px;
      padding: 10px;
      margin-right: 15px;
      box-shadow: 0px 0px 10px rgba(0,0,0,0.2);
    }
    .active {
      border: 3px solid #535bf2;
    }
    img:hover {
      cursor: pointer;
      box-shadow: 0px 0px 10px rgba(0,0,0,0.2);
    }
    img:first-child {
      margin-left: 0;
    }
    img:last-child {
      margin-right: 0;
    }
  }
}</style>