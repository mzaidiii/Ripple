package org.personal.ripple.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClient {

    private final RestClient restClient;

   public  GitHubClient(@Value("${github.token}") String token ){

       SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
       factory.setConnectTimeout(5000);
       factory.setReadTimeout(60000);

       this.restClient= RestClient.builder()
               .baseUrl("https://api.github.com")
               .defaultHeader("Authorization", "Bearer " + token)
               .defaultHeader("Accept", "application/vnd.github+json")
               .requestFactory(factory)
               .build();
   }

   public CompareResult compare (String repoFullName , String before , String after ){
       return restClient.get()
               .uri("/repos/"+repoFullName+"/compare/"+before+"..."+after)
               .retrieve()
               .body(CompareResult.class);
   }

   public  byte[] downloadZip (String repoFullName , String ref){
       return restClient.get()
               .uri("/repos/" + repoFullName + "/zipball/" + ref)
               .retrieve()
               .body(byte[].class);
   }

}
