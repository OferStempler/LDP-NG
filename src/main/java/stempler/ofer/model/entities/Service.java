package stempler.ofer.model.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;

@Data
@NoArgsConstructor
@Document (collection = "services")
public class Service {

	@Id
	private String  id;
	private int     serviceId;
	private String  serviceName;
	private String  provider;
	private String  consumer;
	private String  uri;

	private int     enabled;
	private String  serviceType;
	private String  requestQueue;
	private String  replyQueue;
	private Integer expiry;
	private Integer timeOut;
	private String  contentType; //SOAP | XMLNOSOAP | JSON  - rename version of communicationType;

	private int     forwardClientIp;

	private String  destination;
	private String  destinationType; // MQ | REST | WS
	private Integer persistence;
	
	public Service(int serviceId, String serviceName) {
		this.serviceId = serviceId;
		this.serviceName = serviceName;
	}
}
