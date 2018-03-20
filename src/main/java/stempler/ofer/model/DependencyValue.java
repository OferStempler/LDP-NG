package stempler.ofer.model;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class DependencyValue {

	private String name;
	private String value;
	
	public DependencyValue() {}
	
	public DependencyValue(String name, String value) {
		setName(name);
		setValue(value);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	


}
