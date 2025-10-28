<template>
  <div class="io">
    <el-tabs v-model="activeName" class="demo-tabs">
      <el-tab-pane label="工作区" name="config">
        <div class="scbox" style="margin-top: 0px">
          <div class="scbox-title">生图模型</div>
          <div class="smodel-list">
            <div :class="['smodel-item', 'real', form.model === 1 ? 'active' : '']" @click="selectModel(1)">
              <div class="smodel-item-name">真实</div>
            </div>
            <div :class="['smodel-item', 'ecy', form.model === 2 ? 'active' : '']"  @click="selectModel(2)">
              <div class="smodel-item-name">二次元</div>
            </div>
          </div>
        </div>
        <div class="scbox">
          <div class="scbox-title">基本设置</div>
          <div class="scbox-form">
            <el-form :model="form" label-width="auto">
              <el-form-item label="比例">
                <el-radio-group v-model="form.scale" size="small">
                  <el-radio-button label="1:1" :value="1" />
                  <el-radio-button label="3:4" :value="2" />
                  <el-radio-button label="4:3" :value="3" />
                </el-radio-group>
              </el-form-item>
              <el-form-item label="张数">
                <el-radio-group v-model="form.size" size="small">
                  <el-radio-button label="1张" :value="1" />
                  <el-radio-button label="2张" :value="2" />
                  <el-radio-button label="3张" :value="3" />
                  <el-radio-button label="4张" :value="4" />
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="scbox">
          <div class="scbox-title">高级设置</div>
          <div class="scbox-form">
            <el-form :model="form" label-width="auto">
              <el-form-item label="步数">
                <el-slider v-model="form.step" :max="30" :min="1" :step="4" show-stops/>
              </el-form-item>
              <el-form-item label="强度">
                <el-slider v-model="form.cfg" :max="10" :min="1" :step="1" show-stops/>
              </el-form-item>
              <el-form-item label="采样">
                <el-select v-model="form.sampler" placeholder="请选择采样器" size="small">
                  <el-option
                    v-for="item in samplerOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="种子">
                <el-input v-model="form.seed" size="small"/>
              </el-form-item>
              <el-form-item label="负词">
                <el-input v-model="form.reverse" type="textarea" :rows="2" size="small"/>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="历史记录" name="History"></el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup lang="ts">
import {ref} from "vue";

const activeName = ref('config')
const form= ref({
  size: 1,
  model:1,
  scale:1,
  step:25,
  cfg:8,
  sampler: 1,
  seed: 0,
  reverse:""
});
const samplerOptions = [
  {
    value: 1,
    label: 'DPM++ SDE Karras'
  },
  {
    value: 2,
    label: 'DPM++ 2S Karras'
  },
  {
    value: 3,
    label: 'Euler Karras'
  },
  {
    value: 4,
    label: 'DPM++ 3M SDE Karras'
  }
]
function selectModel(idx){
  form.value.model = idx;
}
function getFormData(){
  return form.value
}
defineExpose({getFormData})
</script>

<style scoped>
.io {
  width: 300px;
  height: 100%;
  background-color: #FFFFFF;
  border-radius: 8px;
  padding: 8px 15px;
  box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.1);
  .scbox {
    margin-top: 12px;
    width: 100%;
    padding-bottom: 10px;
    background-image: linear-gradient(270deg, #EEEFFF 9%, #EFF5FF 93%);
    border-radius: 10px;
    .scbox-form {
      padding: 0px 15px;
    }
    .scbox-title {
      font-family: PingFangSC-Medium;
      font-weight: 500;
      font-size: 14px;
      color: #5555FF;
      width: 100%;
      padding: 10px 15px;
    }
  }
  .smodel-list {
    display: flex;
    flex-direction: row;
    padding: 0px 15px;
    justify-content: space-between;

    .smodel-item {
      border-radius: 8px;
      width: 112px;
      height: 80px;
      background-size: 100% 100%;
      display: flex;
      cursor: pointer;
      flex-direction: column-reverse;
      align-items: center;
      .smodel-item-name {
        font-family: PingFangSC-Regular;
        font-weight: 400;
        font-size: 13px;
        color: #FFFFFF;
        border-radius: 10px;
        padding: 3px 10px;
        background-color: rgba(0, 0, 0, 0.5);
        margin-bottom: 10px;
      }
    }
  }
  .real {
    background: url("../assets/model/model_real.webp");
  }
  .ecy {
    background: url("../assets/model/model_anime.webp");
  }
  .active {
    border: 3px solid #5555FF;
    border-radius: 10px;
  }
}
</style>