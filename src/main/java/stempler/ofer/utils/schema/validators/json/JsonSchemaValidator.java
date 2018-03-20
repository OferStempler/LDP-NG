package stempler.ofer.utils.schema.validators.json;

import java.util.ArrayList;
import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Utility class: JsonSchemaValidator - Json Schema Validator
 * @author yosilev
 * @since March, 2017
 *  
 * 
 */
public class JsonSchemaValidator {

	//-----------------------------------------------------------------------------------------------------------------
	public JsonSchemaValidator(){}
	//-----------------------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param json   String
	 * @param schema String
	 * @return boolean true
	 *   OR
	 * @throws JsonSchemaValidationException
	 *
	 * USAGE EXAMPLE:
	 * 		try {
	 * 			valid = JsonSchemaValidator.validateJsonSchema(json, schema);
	 * 		} catch (JsonSchemaValidationException e) {
	 * 			System.out.println("schema validated failed message: \n" + e.getMessage());
	 * 			System.out.println("schema validated MESSAGES: " + e.getSchemaValidationErrorMessages() );
	 *          valid = false;
	 * 		}
	 * 		System.out.println("schema validated: " + valid);
	 */
	public static boolean validateJsonSchema(String jsonStr, String schemaStr) throws JsonSchemaValidationException{
		JSONObject jsonSchema  = new JSONObject(new JSONTokener(schemaStr));
		JSONObject jsonSubject = new JSONObject(new JSONTokener(jsonStr) );
		
		Schema schema = SchemaLoader.load(jsonSchema);
		
		try {
			schema.validate(jsonSubject);
		} catch (ValidationException e) {
			List<String> messages = new ArrayList<>();
			e.getCausingExceptions().stream().map(ValidationException::getMessage).forEach((s)->messages.add("" + messages.size() + ")" + s));
			throw new JsonSchemaValidationException(e.getMessage(), messages);
		}
		return true;
	}
	//-----------------------------------------------------------------------------------------------------------------
}
