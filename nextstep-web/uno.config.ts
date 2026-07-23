import { defineConfig, presetAttributify, presetIcons, presetUno } from 'unocss'

export default defineConfig({
  presets: [
    presetUno(),
    presetAttributify(),
    presetIcons({ scale: 1.2 })
  ],
  shortcuts: {
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'page': 'p-4 md:p-6 lg:p-8',
    'card-hover': 'transition hover:shadow-md cursor-pointer'
  },
  theme: {
    colors: {
      brand: {
        DEFAULT: '#409EFF',
        dark: '#337ECC'
      }
    }
  }
})
