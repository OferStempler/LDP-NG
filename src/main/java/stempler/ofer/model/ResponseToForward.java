package stempler.ofer.model;
import lombok.Data;
import lombok.ToString;
import lombok.extern.log4j.Log4j;

@Log4j
@Data
@ToString
public class ResponseToForward {

	
	private String body;

	

	public ResponseToForward() {
	
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	
}
