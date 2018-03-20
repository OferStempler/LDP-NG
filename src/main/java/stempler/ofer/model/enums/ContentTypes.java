package stempler.ofer.model.enums;

import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Function;

public enum ContentTypes{ // the content-type
    SOAP                (0, "SOAP"),                 
    JSON                (1, "JSON"),
    XMLNOSOAP           (2, "XMLNOSOAP"),
	COMPOSITE           (3, "COMPOSITE");
    
    public Integer code;
    public String  synonym;
    
	private ContentTypes(Integer code, String synonym) {
		this.code    = code;
		this.synonym = synonym;
	}
	
	public static Function<Integer, ContentTypes> getEnumByCode    = (code)->Arrays.stream( ContentTypes.values() ).filter( tp->tp.code == code ).findFirst().get();
	public static Function<String,  ContentTypes> getEnumBySynonym = (syn)->Arrays.stream( ContentTypes.values() ).filter( tp->tp.synonym.equals( syn ) ).findFirst().get();
	
	public static BiPredicate<String,String> requiresRequestConversion  = (s,d)->s.equals(ContentTypes.JSON.synonym      ) && d.equals(ContentTypes.XMLNOSOAP.synonym );
	public static BiPredicate<String,String> requiresResponseConversion = (s,d)->s.equals(ContentTypes.JSON.synonym ) && d.equals(ContentTypes.XMLNOSOAP.synonym );
	public static BiPredicate<String,String> requiresAnyConversion      = (s,d)->requiresRequestConversion.or(requiresResponseConversion).test(s,d);
	
}
