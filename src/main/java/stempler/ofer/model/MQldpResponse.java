package stempler.ofer.model;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
@Component
public class MQldpResponse {

	public static final int ERROR_CODE_OK        = 0;
	public static final int ERROR_CODE_SYS_ERROR = 1;
	public static final int ERROR_CODE_APP_ERROR = 2;
	
	private int errorCode;
	private String errorDesc;
	private String responseXML;
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorDesc() {
		return errorDesc;
	}
	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}
	public String getResponseXML() {
		return responseXML;
	}
	public void setResponseXML(String responseXML) {
		this.responseXML = responseXML;
	}
	public static int getErrorCodeOk() {
		return ERROR_CODE_OK;
	}
	public static int getErrorCodeSysError() {
		return ERROR_CODE_SYS_ERROR;
	}
	public static int getErrorCodeAppError() {
		return ERROR_CODE_APP_ERROR;
	}
	

}
