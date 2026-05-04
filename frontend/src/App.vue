<template>
  <el-config-provider>
    <div class="app-shell">
      <AppHeader v-if="showHeader" />
      <main :class="showHeader ? 'page-main' : 'auth-main'">
        <router-view />
      </main>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'

const route = useRoute()
const showHeader = computed(() => !['Login', 'Register'].includes(route.name))
</script>
