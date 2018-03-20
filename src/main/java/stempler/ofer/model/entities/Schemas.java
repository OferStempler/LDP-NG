package stempler.ofer.model.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;

@Getter 
@Setter 
@RequiredArgsConstructor 
@EqualsAndHashCode
@Document (collection = "schemas")
public class Schemas  {

	@Id 
	private String id;
	private String schemaType;
	private String schema;
	private int serviceId;
	@Override
	public String toString() {
		return "Schemas [id=" + id + ", schemaType=" + schemaType + ", schema.length()=" + schema.length() + ", serviceId=" + serviceId
				+ "]";
	}
	
}
