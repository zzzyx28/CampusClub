# AI Provider Configuration

CampusClub keeps the Agent layer independent from the model vendor. The business code uses Spring AI `ChatClient`, memory advisors, tool callbacks and vector store abstractions, while the concrete model provider is selected by configuration.

## DashScope Native Mode

Use Alibaba's native Spring AI starter:

```yaml
app:
  ai:
    provider: dashscope
    tool-calling-enabled: true
    chat:
      model: qwen-plus
    embedding:
      model: text-embedding-v1
    dashscope:
      enabled: true
    openai-compatible:
      enabled: false
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
```

## OpenAI-Compatible Mode

Use OpenAI protocol compatible providers, including Aliyun compatible-mode, One API or LiteLLM:

```yaml
app:
  ai:
    provider: openai-compatible
    # Some compatible streaming endpoints emit function arguments in chunks that older Spring AI versions parse too early.
    # Disable tool calling for stable streaming demos; local business snapshots are injected as fallback context.
    tool-calling-enabled: false
    chat:
      model: qwen-plus
    embedding:
      model: text-embedding-v1
    dashscope:
      enabled: false
    openai-compatible:
      enabled: true
      api-key: ${OPENAI_COMPATIBLE_API_KEY}
      base-url: https://llm-xa2inbbuopkt0upv.cn-beijing.maas.aliyuncs.com/compatible-mode/v1

spring.ai:
  openai:
    api-key: ${app.ai.openai-compatible.api-key}
    base-url: ${app.ai.openai-compatible.base-url}
    chat:
      enabled: true
      completions-path: /chat/completions
      options:
        model: ${app.ai.chat.model}
    embedding:
      enabled: true
      embeddings-path: /embeddings
      options:
        model: ${app.ai.embedding.model}
```

The Aliyun compatible URL already ends with `/v1`, so the Spring AI OpenAI paths are configured as `/chat/completions` and `/embeddings` to avoid producing `/v1/v1/...`.

## Resume Highlight

- Built provider-switchable AI infrastructure with Spring AI.
- Supports DashScope native SDK and OpenAI-compatible endpoints without changing Agent business code.
- Keeps memory, RAG and tool-calling stable across model providers.
- Exposes selected provider and model names through `/actuator/info` for runtime observability.
