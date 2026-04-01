<script setup lang="ts">

import Hero from "../components/main/hero.vue";
import About from "../components/main/about.vue";
import FAQ from "../components/main/faq.vue";
import Footer from "../components/main/footer.vue";
import Header from "../components/main/header.vue";


</script>
<template>
  <div class="min-h-screen flex flex-col overflow-x-clip">
    <Header />
    <main class="flex-1">
      <Hero />
      <About />
      <FAQ />
    </main>

    <Footer />
  </div>
</template>