<script setup lang="ts">

import Hero from "../components/hero.vue";
import About from "../components/about.vue";
import FAQ from "../components/faq.vue";
import Footer from "../components/footer.vue";
import Header from "../components/header.vue";


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