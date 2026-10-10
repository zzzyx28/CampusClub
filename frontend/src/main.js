import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import './assets/main.css'
import {
  ArrowLeft,
  ChatDotRound,
  ChatLineRound,
  DataBoard,
  Delete,
  Document,
  GoldMedal,
  List,
  Loading,
  Location,
  Lock,
  Medal,
  Message,
  Phone,
  Plus,
  Search,
  Service,
  Setting,
  Star,
  Ticket,
  Trophy,
  User,
  UserFilled
} from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

const icons = {
  ArrowLeft, ChatDotRound, ChatLineRound, DataBoard, Delete, Document,
  GoldMedal, List, Loading, Location, Lock, Medal, Message, Phone, Plus,
  Search, Service, Setting, Star, Ticket, Trophy, User, UserFilled
}

for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
