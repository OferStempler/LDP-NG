package stempler.ofer.model.enums;

import java.util.Arrays;
import java.util.function.Function;

public enum XSDType { // the content-type
	GENERAL        			(0, "GENERAL"),
	GENERIC_DATA			(1, "GENERIC_DATA"),
    RESPONSE_HEADER         (2, "RESPONSE_HEADER"),
    REQUEST_HEADER          (3, "REQUEST_HEADER"),
    XSD1					(4, "XSD1"),
    XSD2					(5, "XSD2"),
    XSD3					(6, "XSD3"),

    SERVICE                 (10, "SERVICE");               
    
    public Integer code;
    public String  synonym;
    
	private XSDType(Integer code, String synonym) {
		this.code    = code;
		this.synonym = synonym;
	}
	
	public static Function<Integer, XSDType> getEnumByCode    = (code)->Arrays.stream( XSDType.values() ).filter( tp->tp.code == code ).findFirst().get();
	public static Function<String,  XSDType> getEnumBySynonym = (syn)->Arrays.stream( XSDType.values() ).filter( tp->tp.synonym.equals( syn ) ).findFirst().get();
	
}
