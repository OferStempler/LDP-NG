package stempler.ofer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;

import javax.servlet.http.HttpServletResponse;

@Getter
@Setter
@ToString(callSuper=true)
@AllArgsConstructor
public class LdpResponseExtended extends LdpResponse {
	
	private String content;
	private String origContentResponse;
	private LinkedMultiValueMap<String, String> headers;
//	private HttpHeaders httpHeaders;
	private HttpServletResponse response;
	
	public LdpResponseExtended() {	super(); }
	public LdpResponseExtended(HttpStatus responseCode, String responseMessage) {
		super(responseCode, responseMessage);
	}

	public LdpResponseExtended(HttpStatus responseCode, String responseMessage, String content) {
		super(responseCode, responseMessage);
		this.content = content;
	}
//	public LdpResponseExtended(HttpStatus responseCode, String responseMessage, String content, HttpHeaders httpHeaders) {
//		super(responseCode, responseMessage);
//		this.content     = content;
//		this.httpHeaders = httpHeaders;
//	}
	public LdpResponseExtended(HttpServletResponse response, HttpStatus responseCode, String responseMessage, String content) {
	super(responseCode, responseMessage);
	this.content = content;
}

//	public LdpResponseExtended(HttpHeaders httpHeaders, HttpStatus responseCode, String responseMessage, String content) {
//		super(responseCode, responseMessage);
//		this.content = content;
//	}

	public LdpResponse toLdpResponse(LdpResponseExtended ldpResponseExtended){
		return new LdpResponse(ldpResponseExtended.getResponseCode(), ldpResponseExtended.getResponseMessage());
	}
	public LdpResponseExtended(HttpStatus responseCode, String responseMessage, String content, LinkedMultiValueMap<String, String> headers) {
	super(responseCode, responseMessage);
	this.content = content;
	this.headers = headers;
}
	
}
