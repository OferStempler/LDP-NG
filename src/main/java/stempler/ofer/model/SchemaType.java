package stempler.ofer.model;

import org.springframework.stereotype.Component;

import lombok.Data;


@Data
@Component
public class SchemaType {

	private Integer seq;
	private String schema;
	
	public SchemaType() {}
	
	public SchemaType(Integer type, String schema) {
		setSeq(type);
		setSchema(schema);
	}




}
