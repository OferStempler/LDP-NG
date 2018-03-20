package stempler.ofer.model.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
@Data
@Document (collection = "serviceRegularExpressions")
public class ServiceRegularExpressions {
	
	@Id
	private String id;
	private int    serviceId;
	private String messageType;
	private String element;
	private int    regexId;
	private int    enabled;
}
