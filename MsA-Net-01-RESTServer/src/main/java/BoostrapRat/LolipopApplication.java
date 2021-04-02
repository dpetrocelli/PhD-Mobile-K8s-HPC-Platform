package BoostrapRat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// https://memorynotfound.com/spring-boot-passing-command-line-arguments-example/
/*
@SpringBootApplication
public class LolipopApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(LolipopApplication.class, args);
	}
}
*/
@SpringBootApplication
public class LolipopApplication {

    private static final Logger logger = LoggerFactory.getLogger(LolipopApplication.class);

    public static void main(String... args) throws Exception {
        SpringApplication.run(LolipopApplication.class, args);
    }


}