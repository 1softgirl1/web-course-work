<script setup lang="ts">

import Hero from "../components/main/hero.vue";
import About from "../components/main/about.vue";
import FAQ from "../components/main/faq.vue";
import Footer from "../components/main/footer.vue";
import Header from "../components/main/header.vue";
import { onMounted } from "vue";
import { deviceType, referrerSource, track } from "@/api/telemetry";

onMounted(() => {
  track("public_page_opened", { metadata: { page: "main", referrer_source: referrerSource(), device_type: deviceType() } })
})


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