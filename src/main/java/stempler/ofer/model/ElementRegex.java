package stempler.ofer.model;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ElementRegex {

	private String element;
	private String regexName;
	private String regex;
	private int enabled;
	
	public ElementRegex() {}

	public ElementRegex(String element, String regexName, String regex, int enabled) {
		super();
		this.element = element;
		this.regexName = regexName;
		this.regex = regex;
		this.enabled = enabled;
	}	

}
