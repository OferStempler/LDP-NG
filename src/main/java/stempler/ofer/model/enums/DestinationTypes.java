package stempler.ofer.model.enums;

import java.util.Arrays;
import java.util.function.Function;

public enum DestinationTypes{ 
    MQ   (0, "MQ"),                 
    REST (1, "REST"),
    WS   (2, "WS");
    
    public Integer code;
    public String  synonym;
    
	private DestinationTypes(Integer code, String synonym) {
		this.code    = code;
		this.synonym = synonym;
	}
	
	public static Function<Integer, DestinationTypes> getEnumByCode    = (code)->Arrays.stream( DestinationTypes.values() ).filter( tp->tp.code == code ).findFirst().get();
	public static Function<String,  DestinationTypes> getEnumBySynonym = (syn)->Arrays.stream( DestinationTypes.values() ).filter( tp->tp.synonym.equals( syn ) ).findFirst().get();
}
