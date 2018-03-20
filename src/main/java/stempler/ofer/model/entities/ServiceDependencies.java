package stempler.ofer.model.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;

@Data
@Document (collection = "servicesDependencies")
public class ServiceDependencies {

	@Id
	private String id;
	private int    serviceId;
	private String messageType;
	private int    dependencyId;
	private String dependencyValue;
	private float additionalInfo;
	private int    enabled;
}
