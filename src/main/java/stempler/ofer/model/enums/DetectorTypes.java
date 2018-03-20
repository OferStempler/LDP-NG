package stempler.ofer.model.enums;

import java.util.Arrays;
import java.util.function.Function;

public enum DetectorTypes{ 
	REGEX_VALIDATION        (1, "REGEX_VALIDATION"),                 
	DEPTH_VALIDATION     (2, "DEPTH_VALIDATION "),
	SIZE_VALIDATION      (3, "SIZE_VALIDATION "),
	SCHEMA_VALIDATION       (4, "SCHEMA_VALIDATION"),
	FILE_SIZE_VALIDATION	(5, "FILE_SIZE_VALIDATION"),
	FILESANITIZE_VALIDATION (6, "FILESANITIZE_VALIDATION"),
//	CONVERT_CONTENT_TYPE   (6, "CONVERT_CONTENT_TYPE"),
	REPLACE_FIELD_CONTENT    (7, "REPLACE_FIELD_CONTENT")
	;
    
    public Integer code;
    public String  synonym;
    
	private DetectorTypes(Integer code, String synonym) {
		this.code    = code;
		this.synonym = synonym;
	}
	
	public static Function<Integer, DetectorTypes> getEnumByCode    = (code)->Arrays.stream( DetectorTypes.values() ).filter( tp->tp.code == code ).findFirst().get();
	public static Function<String,  DetectorTypes> getEnumBySynonym = (syn)->Arrays.stream( DetectorTypes.values() ).filter( tp->tp.synonym.equals( syn ) ).findFirst().get();
}
