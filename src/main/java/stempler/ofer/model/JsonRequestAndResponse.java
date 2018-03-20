package stempler.ofer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class JsonRequestAndResponse {
	
	private int serviceId;
	private Object request;
	private Object response;

}
