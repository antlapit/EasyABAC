package custis.easyabac.core.trace.model;

/**
 * Policy with trace
 */
public class CalculatedRule {

    private final String id;
    private CalculatedResult result;
    private CalculatedMatch match;


    public CalculatedRule(String id) {
        this.id = id;
    }

    public void setMatch(CalculatedMatch match) {
        this.match = match;
    }

    public void setResult(CalculatedResult result) {
        this.result = result;
    }
}
