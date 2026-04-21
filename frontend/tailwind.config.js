import animate from "tailwindcss-animate";

export default {
    content: ["./index.html", "./src/**/*.{vue,js,ts}"],
    theme: {
        extend: {},
    },
    plugins: [animate],
};