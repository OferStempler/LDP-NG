package stempler.ofer.utils.schema.validators.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonSchemaValidationException extends Exception{

	private static final long serialVersionUID = 1L;
	private List<String> schemaValidationErrorMessages = new ArrayList<>();
	
	public JsonSchemaValidationException() {}
	public JsonSchemaValidationException(String message) {
		super(message);
	}
	public JsonSchemaValidationException(String message, List<String> schemaValidationErrorMessages ) {
		this(message);
		this.schemaValidationErrorMessages = schemaValidationErrorMessages;
	}
	
	public List<String> getSchemaValidationErrorMessages() {
		return schemaValidationErrorMessages;
	}

	@Override
	public String getMessage(){
		StringBuffer sb = new StringBuffer();
		Optional.ofNullable(schemaValidationErrorMessages).ifPresent( (l)->l.stream().map(m->m).forEach((s)->sb.append(s).append("\n")) );
		return sb.toString();
	}

}
