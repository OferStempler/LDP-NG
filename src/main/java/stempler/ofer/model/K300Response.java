package stempler.ofer.model;

import lombok.Data;

@Data
public class K300Response {

	private Boolean isSuccess;
	private String details;
	private String guid;
	private int errorCode;
	private int SanitizeCode;
	
}
