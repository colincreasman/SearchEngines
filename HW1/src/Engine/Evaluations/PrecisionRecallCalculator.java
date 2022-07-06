package Engine.Evaluations;


// TODO: implement all the methods we might need to plot a full precision-recall curve and calculate the MAP over an entire set of queries
public class PrecisionRecallCalculator {
    private EvaluatedQuery mQueryRel; // a single

    public PrecisionRecallCalculator() {}

    public PrecisionRecallCalculator(EvaluatedQuery q) {
        mQueryRel = q;
    }

    // caculates basic precision using(# rel docs) /  items retrieved
}
