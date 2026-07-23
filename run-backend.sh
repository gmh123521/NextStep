#!/usr/bin/env bash
# 启动 nextstep-api 后端（自动加载 .env.local 中的环境变量）
# 用法：bash run-backend.sh

set -e

cd "$(dirname "$0")"

# 加载 .env.local（如果存在）
if [ -f .env.local ]; then
  echo "[run] loading .env.local"
  set -a
  source .env.local
  set +a
fi

if [ -z "$DASHSCOPE_API_KEY" ] || [[ "$DASHSCOPE_API_KEY" == *"粘贴到这里"* ]]; then
  echo "[警告] DASHSCOPE_API_KEY 未配置或仍是占位符 — LLM 接口将无法工作"
  echo "       请编辑 .env.local 把 sk-xxx 真实 key 填进去"
fi

# 杀掉旧实例
OLD_PID=$(netstat -ano 2>/dev/null | grep LISTENING | grep ":8080 " | awk '{print $5}' | head -1)
if [ -n "$OLD_PID" ]; then
  echo "[run] killing old PID $OLD_PID on :8080"
  cmd //c "taskkill /F /PID $OLD_PID" >/dev/null 2>&1 || true
  sleep 2
fi

JAR=nextstep-api/target/nextstep-api.jar
if [ ! -f "$JAR" ]; then
  echo "[run] jar not found, packaging first..."
  mvn package -DskipTests -pl nextstep-api -am -q
fi

echo "[run] starting nextstep-api on :8080 ..."
nohup java -jar "$JAR" > app.log 2>&1 &
echo "[run] PID=$!  log=app.log"
