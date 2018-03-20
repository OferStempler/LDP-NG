package stempler.ofer.service;


import stempler.ofer.model.ExtendedService;

public interface AuditService {

	
	public void requestResponseBuildAudit (ExtendedService extendedService, String content, String status, String id);
	public void generalAudit (String content, String status, String error);


}
