package tech.aurora.bfadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
// import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter; // Protobuf removed
import org.springframework.web.client.RestTemplate;

@ServletComponentScan
@SpringBootApplication
public class BuildfarmAdmin {

  public static void main(String[] args) {
    SpringApplication.run(BuildfarmAdmin.class, args);
  }

  @Bean
  RestTemplate restTemplate() {
    // Protobuf converter removed
    return new RestTemplate();
  }

  // Protobuf converter bean removed
  // @Bean
  // ProtobufHttpMessageConverter protobufHttpMessageConverter() {
  //   return new ProtobufHttpMessageConverter();
  // }
}
