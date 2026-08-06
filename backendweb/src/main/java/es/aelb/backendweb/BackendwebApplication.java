package es.aelb.backendweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendwebApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendwebApplication.class, args);
	}

}
