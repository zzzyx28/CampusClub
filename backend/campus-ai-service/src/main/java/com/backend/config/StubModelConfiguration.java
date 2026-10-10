package com.backend.config;
import org.springframework.context.annotation.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.embedding.*;
import org.springframework.ai.document.Document;
import java.util.*;
@Configuration @Profile("stub")
public class StubModelConfiguration {
 @Bean @Primary ChatModel stubChatModel(){return new ChatModel(){
  public ChatOptions getDefaultOptions(){return ChatOptionsBuilder.builder().build();}
  public ChatResponse call(Prompt prompt){return new ChatResponse(List.of(new Generation("[本地模型替身] 此回答仅用于验证接口、SSE 与服务调用。")));}
  public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt){return reactor.core.publisher.Flux.just(call(prompt));}
 };}
 @Bean @Primary EmbeddingModel stubEmbeddingModel(){return new EmbeddingModel(){
  public int dimensions(){return 8;}
  public float[] embed(Document doc){return vector(doc.getContent());}
  public EmbeddingResponse call(EmbeddingRequest request){List<Embedding> results=new ArrayList<>();for(int i=0;i<request.getInstructions().size();i++)results.add(new Embedding(vector(request.getInstructions().get(i)),i));return new EmbeddingResponse(results);}
  private float[] vector(String text){float[] values=new float[8];values[0]=1;for(int i=0;i<text.length();i++)values[Math.floorMod(text.charAt(i),8)]+=1;return values;}
 };}
}
