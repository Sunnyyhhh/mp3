<template>
  <div id="app">
    <RouterView />
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  background: #f5f7fb;
  color: #2d3748;
  min-height: 100vh;
}

#app{
  min-height:100vh;
}

::-webkit-scrollbar{
    width:8px;
}

::-webkit-scrollbar-track{
    background:#e5e7eb;
}

::-webkit-scrollbar-thumb{
    background:#8b5cf6;
    border-radius:10px;
}
</style>