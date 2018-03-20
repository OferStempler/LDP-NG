package stempler.ofer.detectors;

import stempler.ofer.model.ExtendedService;
import stempler.ofer.model.entities.Service;
import stempler.ofer.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Component
public abstract class Detector implements Comparable<Detector> {


	protected int priority;

	@Autowired
	protected AuditService auditService;
	
	public abstract boolean execute(ExtendedService extendedService, StringBuffer content, String messageType, String id, HttpServletRequest servletRequest );
	public abstract void    init();

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int compareTo(Detector other){
		return this.priority-other.priority;
	}
	
	protected String composeServiceIdentifier(ExtendedService extendedService){
		Service service = Optional.ofNullable(extendedService.getService()).orElseThrow(()->new RuntimeException("Missing Service for ExtendedService- shouuld not happen") );
		return "id:[" + service.getServiceId() + "], name:[" + service.getServiceName() + "]"; 
	}
}
