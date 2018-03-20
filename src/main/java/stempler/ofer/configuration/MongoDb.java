package stempler.ofer.configuration;

import stempler.ofer.utils.GeneralUtils;
import lombok.extern.log4j.Log4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb")
@Log4j
public class MongoDb{
	public String host;
	public String port;
	public String database;
	public String username;
	public String password;
	
	
	@PostConstruct
	public void init() {
		log.debug("MongoDb.CTOR");
		Optional.ofNullable(host).orElseThrow(()->new RuntimeException("MongoDb.init() - mongoDB host parameter should not be null - check application.yml config file."));
		Optional.ofNullable(port).orElseThrow(()->new RuntimeException("MongoDb.init() - mongoDB port parameter should not be null - check application.yml config file."));
		if ( !GeneralUtils.isReachable(host, Integer.valueOf( port ) ) ){
			throw new RuntimeException("MongoDb.init() - FATAL mongoDB server (host:[" + host + "], port:[" + port + "]) is not reachable!");
		}
	}
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getDatabase() {
		return database;
	}
	public void setDatabase(String database) {
		this.database = database;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
}
