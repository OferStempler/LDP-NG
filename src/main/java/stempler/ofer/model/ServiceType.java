package stempler.ofer.model;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ServiceType {

	private String serviceName;
	private boolean enabled;
	
	public ServiceType() {}
	
	public ServiceType(String serviceName, boolean enabled) {
		setEnabled(enabled);
		setServiceName(serviceName);
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	
	
}
