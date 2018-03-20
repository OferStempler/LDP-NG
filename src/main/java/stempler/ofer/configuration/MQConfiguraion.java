package stempler.ofer.configuration;

import lombok.extern.log4j.Log4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "MQ")
@Log4j
public class MQConfiguraion {

	private String host;
	private String qmanager;
	private String channel;
	private String port;
	private String maxConnections;
	private String numUnusedConnections;
	private String timeout;
	private Map<String, String> MQEnvironmentMap;
	// @NestedConfigurationProperty
	// MQEnvironmentMap mQEnvironmentMap;
	private String   mqConnectionPoolLoadOnStartup;
	
	@PostConstruct
	public void init(){
		log.debug("MQConfiguraion.CTOR");
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getQmanager() {
		return qmanager;
	}
	public void setQmanager(String qmanager) {
		this.qmanager = qmanager;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getMaxConnections() {
		return maxConnections;
	}
	public void setMaxConnections(String maxConnections) {
		this.maxConnections = maxConnections;
	}
	public String getNumUnusedConnections() {
		return numUnusedConnections;
	}
	public void setNumUnusedConnections(String numUnusedConnections) {
		this.numUnusedConnections = numUnusedConnections;
	}
	public String getTimeout() {
		return timeout;
	}
	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}
	public String getMqConnectionPoolLoadOnStartup() {
		return mqConnectionPoolLoadOnStartup;
	}
	public void setMqConnectionPoolLoadOnStartup(
			String mqConnectionPoolLoadOnStartup) {
		this.mqConnectionPoolLoadOnStartup = mqConnectionPoolLoadOnStartup;
	}
	public Map<String, String> getMQEnvironmentMap() {
		return MQEnvironmentMap;
	}
	public void setMQEnvironmentMap(Map<String, String> mQEnvironmentMap) {
		MQEnvironmentMap = mQEnvironmentMap;
	}
	
}
