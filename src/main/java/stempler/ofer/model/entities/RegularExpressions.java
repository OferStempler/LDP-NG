package stempler.ofer.model.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
@Data
@Document(collection = "regularExpressions")
public class RegularExpressions {

	@Id
	private String id;
	private int regexId;
	private String name;
	private String value;
}
