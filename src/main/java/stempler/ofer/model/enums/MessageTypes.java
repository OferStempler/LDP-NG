package stempler.ofer.model.enums;

import java.util.Arrays;
import java.util.function.Function;

public enum MessageTypes{ // the content-type
    REQUEST                (0, "Request"),                 
    RESPONSE                (1, "Response"),;
    
    public Integer code;
    public String  synonym;
    
	private MessageTypes(Integer code, String synonym) {
		this.code    = code;
		this.synonym = synonym;
	}
	
	public static Function<Integer, MessageTypes> getEnumByCode    = (code)->Arrays.stream( MessageTypes.values() ).filter( tp->tp.code == code ).findFirst().get();
	public static Function<String,  MessageTypes> getEnumBySynonym = (syn)->Arrays.stream( MessageTypes.values() ).filter( tp->tp.synonym.equals( syn ) ).findFirst().get();
	
}
