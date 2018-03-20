package stempler.ofer.service;

import org.springframework.web.multipart.MultipartFile;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.LdpResponse;
import stempler.ofer.model.ValidationResponse;
import stempler.ofer.model.entities.Service;
import stempler.ofer.model.entities.ServiceConversions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestResponseService {

	public LdpResponse startValidation(HttpServletRequest request, HttpServletResponse response,  MultipartFile[] attachmentFiles, String body);
	public String      getMessageContent(HttpServletRequest request, String id);
	public String      getXmlNoSOAP(String message, String serviceName, String id);
	public String      forwardClientIP (String message,  Service service, HttpServletRequest request, String id);
	public String      removeClientIPTagFromHeader(String xml,String clientIP);
	public boolean     isEnabled(ExtendedService extendedService, String id);
	public LdpResponse getParameters(HttpServletRequest request, HttpServletResponse response);
	public ValidationResponse validateContentType(String content, String contentType, String serviceName, String guid,  String messageType, ServiceConversions serviceSourceConversions );
	public boolean     reload();
}
