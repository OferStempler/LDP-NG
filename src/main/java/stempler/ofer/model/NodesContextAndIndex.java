package stempler.ofer.model;
import lombok.Data;

@Data
public class NodesContextAndIndex {

	private String nodeContextText;
	private int endOfNodeContext;
	private int newStartOfNodeContext;
}
