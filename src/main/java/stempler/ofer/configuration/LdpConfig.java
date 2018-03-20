package stempler.ofer.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "ldp")
public class LdpConfig {
	
	private String enableAudit;
	private String ipFromReq;
	private String realodUrlFromLdp;
	private String k300UploadFileUrl;
	private String k300GetFileGuidUrl;
}
