package stempler.ofer.utils.schema.validators.json;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * Test class: MainJsonSchemaTester - Json Schema Validator Tester
 * @author yosilev
 * created: March, 2017 
 *
 */
public class MainJsonSchemaTester {

    public static String json = "{ \"id\": 1, \"name\": \"Lampshade\", \"price\": 11, person:{ \"id\":\"123\", \"name\" : \"Yoel\"} }";
    
	//-----------------------------------------------------------------------------------------------------------------
	private static String readSchemaFile(String fileName) throws IOException {
		InputStream is = MainJsonSchemaTester.class.getClassLoader().getResourceAsStream(fileName);
		byte [] buff = new byte[is.available()];
		is.read(buff);
		String schema = new String(buff);
		return schema;
	}
	//-----------------------------------------------------------------------------------------------------------------
	public static void main(String[] args) {
		boolean valid = false;
		String schema = null;
		try {
			schema = readSchemaFile("json_schema1.json");
		} catch (Exception e) {
	        e.printStackTrace();			
		}
		try {
			valid = JsonSchemaValidator.validateJsonSchema(json, schema);
		} catch (JsonSchemaValidationException e) {
			System.out.println("schema validated failed message: \n" + e.getMessage());
			System.out.println("schema validated MESSAGES: " + e.getSchemaValidationErrorMessages() );
		}
		System.out.println("schema validated: " + valid);
	}
	//-----------------------------------------------------------------------------------------------------------------
}
