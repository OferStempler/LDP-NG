package stempler.ofer.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import stempler.ofer.configuration.LdpConfig;
import stempler.ofer.dao.AuditRepository;
import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.entities.Audit;
import stempler.ofer.service.AuditService;

import java.text.SimpleDateFormat;

@Component
@Log4j
public class AuditServiceImpl implements AuditService{


	private final String FAILD = "Failed";
	private final String Succsess = "Succsess";

	
	@Autowired
	private AuditRepository auditRepo;
	@Autowired
	private LdpConfig ldpConfig;
	
	@Override
	public void requestResponseBuildAudit(ExtendedService extendedService, String content, String message, String guid) {
		
		//check for properties if audit save is enabled
		if(ldpConfig.getEnableAudit() != null && Integer.parseInt(ldpConfig.getEnableAudit()) == 1){
		
		Audit audit = auditRepo.findByGuid(guid);
		if (audit != null){
			
			audit.setResponse(content);
			if(!message.equals("") ) {
				audit.setMessage(message);
				audit.setStatus(FAILD);
			}
			audit.set_id(audit.get_id());
			auditRepo.save(audit);
		} else {
		
		audit = new Audit();
		
		if (extendedService != null && content != null ){
			audit.setServiceName(extendedService.getService().getServiceName());
			audit.setUrl(extendedService.getService().getServiceName());
			audit.setDestination(extendedService.getService().getDestination());
			audit.setDate(new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss").format(System.currentTimeMillis()));
			audit.setRequest(content);
			audit.setGuid(guid);
			
			if(message == null || message.equals("")){
				audit.setStatus(Succsess);				
			}
			
			else {
				audit.setMessage(message);
				audit.setStatus(FAILD);
			} 
		}
		
		else if (extendedService == null || content ==null) {
			audit.setServiceName("");
			audit.setUrl("");
			audit.setDestination("");
			audit.setDate(new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss").format(System.currentTimeMillis()));
			audit.setRequest(content);
			audit.setMessage("extendedService or message content are null");
			audit.setStatus(FAILD);
			audit.setGuid(guid);
		}
		auditRepo.insert(audit);
		log.debug("Inserted audit for LDP-Request Guid: [" +guid+"]");
		
		
//		log.debug("Saved audit to db: " + audit.toString());
		
		}
	} else {
		log.debug("Audit is not enabled. To enable auditting, put '1' in application, properties under ldp.enableAudit");
	}
	}

	@Override
	public void generalAudit(String type, String status, String error) {
		//check for properties if audit save is enabled
		if(ldpConfig.getEnableAudit() != null && Integer.parseInt(ldpConfig.getEnableAudit()) == 1){

		Audit audit = new Audit();
		audit.setContent(type);
		audit.setDate(new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss").format(System.currentTimeMillis()));
		
			if(error == null){
				audit.setStatus(Succsess);				
			} else {
				audit.setMessage(error);
				audit.setStatus(FAILD);
			} 
		auditRepo.insert(audit);
		log.debug("Inserted audit for ["+type+"] ");		
		
	} else {
		log.debug("Audit is not enabled. To enable auditting, put '1' in application properties under ldp.enableAudit");
	}
	
		
	}
}
