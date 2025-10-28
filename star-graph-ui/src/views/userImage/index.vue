<template>
  <div class="ilist list">
    <div class="iitem gqmt" v-for="(item,idx) in resultImages" :style="{backgroundImage: 'url(' + item.url + ')',marginRight: ((idx+1) % 5)==0?'0px':'16px'}">
      <div class="footer">
        <div class="itool">
          <div  class="footer-desc" v-if="item.createdTime">
            {{(item.createdTime+"").substring(0,10)}}
          </div>
          <div  class="footer-desc" v-else></div>
          <div :class="['footer-collection', item.collect?'active':'']">
            收藏
          </div>
        </div>
        <div class="ititle"></div>
      </div>
    </div>
  </div>

  <div class="page-body">
    <el-pagination
        v-model:current-page="pageInfo.pageNum"
        v-model:page-size="pageInfo.pageSize"
        :page-sizes="[10, 20]"
        :background="true"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import Text2ImageAPI from "@/api/t2i";
import {onMounted, ref} from "vue";

const resultImages = ref([]);
const total = ref(0);
const pageInfo = ref({
  pageNum: 1,
  pageSize: 10
});

function listImages() {
  Text2ImageAPI.listImages(pageInfo.value).then(res => {
      resultImages.value = res.data
      total.value = res.total
  }).catch(err=>{
  })
}

function handleSizeChange(){
  listImages();
}
function handleCurrentChange(){
  listImages();
}

onMounted(()=>{
  listImages();
})
</script>

<style scoped>
.list {
  margin-top: 17px;
  justify-content: flex-start;
  .iitem {
    .active {
      background-color: #535bf2;
      border-radius: 5px;
    }
  }
}
.page-body {
  width: 100%;
  margin:30px;
  text-align: center;
  justify-content: center;
  display: flex;
}
</style>