<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi, type ChatHistoryMessage } from '@/api/chat'

const emit = defineEmits<{ (e: 'profileChanged'): void }>()

const visible = ref(false)
const messages = ref<ChatHistoryMessage[]>([])
const input = ref('')
const sending = ref(false)
const scrollRef = ref<HTMLDivElement | null>(null)

function open() {
  visible.value = true
  if (!messages.value.length) loadHistory()
}
defineExpose({ open })

async function loadHistory() {
  try {
    const list = await chatApi.history()
    messages.value = list || []
    if (!messages.value.length) {
      // 没有历史 → 让 LLM 看着当前画像开场（不写死话术，避免重复问已填字段）
      await kickoff()
    }
    scrollToBottom()
  } catch {}
}

/** 启动语：让 LLM 看画像后主动开口 */
async function kickoff() {
  sending.value = true
  try {
    const r = await chatApi.kickoff()
    messages.value = [{ role: 'assistant', content: r.reply }]
  } catch {
    messages.value = [{ role: 'assistant', content: '你好，我是 NextStep 学长助手，有什么想聊的？' }]
  } finally {
    sending.value = false
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  sending.value = true
  scrollToBottom()
  try {
    const r = await chatApi.send(text)
    messages.value.push({ role: 'assistant', content: r.reply })
    if (r.profileUpdated || r.experienceAdded) {
      emit('profileChanged')
    }
    // 不弹 toast：学长式对话要求操作隐形，用户感知不到"字段被更新"
    // if (r.updatedFields?.length) { ElMessage.success(...) }
    scrollToBottom()
  } catch (e: any) {
    messages.value.push({ role: 'assistant', content: '（出错了：' + (e?.msg || e?.message || '请重试') + '）' })
  } finally {
    sending.value = false
  }
}

async function reset() {
  try {
    await ElMessageBox.confirm('清空当前对话历史？已写入画像的数据不会被删除。', '重置对话', {
      confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning'
    })
    await chatApi.reset()
    messages.value = []
    loadHistory()
  } catch { /* 取消 */ }
}

function scrollToBottom() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  })
}

function onEnter(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  send()
}

function asText(content: any): string {
  if (content == null) return ''
  if (typeof content === 'string') return content
  if (Array.isArray(content)) return content.map(c => c.text || '').join('')
  return String(content)
}
</script>

<template>
  <el-drawer
    v-model="visible"
    title="AI 学长聊聊（帮你补齐画像）"
    direction="rtl"
    :size="480"
    :with-header="true"
  >
    <div class="flex flex-col h-full">
      <div ref="scrollRef" class="flex-1 overflow-y-auto space-y-3 pr-1">
        <div v-for="(m, i) in messages" :key="i" class="flex" :class="m.role === 'user' ? 'justify-end' : 'justify-start'">
          <div
            class="max-w-80 px-3 py-2 rounded-lg text-sm leading-relaxed whitespace-pre-wrap"
            :class="m.role === 'user'
              ? 'bg-brand text-white rounded-br-sm'
              : 'bg-gray-100 text-gray-800 rounded-bl-sm'"
          >
            {{ asText(m.content) }}
          </div>
        </div>
        <div v-if="sending" class="flex justify-start">
          <div class="bg-gray-100 px-3 py-2 rounded-lg text-sm text-gray-500">
            <span class="inline-block w-2 h-2 bg-gray-400 rounded-full animate-pulse" />
            <span class="inline-block w-2 h-2 bg-gray-400 rounded-full animate-pulse ml-1" style="animation-delay:.15s" />
            <span class="inline-block w-2 h-2 bg-gray-400 rounded-full animate-pulse ml-1" style="animation-delay:.3s" />
          </div>
        </div>
      </div>

      <div class="border-t border-gray-100 pt-3 mt-3">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          placeholder="输入消息，Enter 发送 / Shift+Enter 换行"
          resize="none"
          @keydown.enter="onEnter"
        />
        <div class="flex justify-between items-center mt-2">
          <el-button text size="small" @click="reset">清空对话</el-button>
          <el-button type="primary" size="small" :loading="sending" :disabled="!input.trim()" @click="send">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>
