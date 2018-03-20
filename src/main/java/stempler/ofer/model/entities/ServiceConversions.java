package stempler.ofer.model.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;

@Data
@Document (collection = "serviceConversions")
public class ServiceConversions {
	
	@Id
	private String id;
	private int serviceId;
	
	//request
	private String sourceRequestInputType;
	private String destinationRequestInputType;
	
	//response
	private String destinationResponseInputType;
	private String sourceResponseInputType;
	private int enabled;
}
