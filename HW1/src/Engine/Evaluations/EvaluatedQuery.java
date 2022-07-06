package Engine.Evaluations;

import Engine.Queries.RankedQuery;

import java.util.List;

// TODO: implement this so that it encapsulates a single RankedQuery object and a matching List<Integer> of relevant docId's
public class EvaluatedQuery {
    private RankedQuery mQuery;

    private List<String> mTotalRelevant; // list of all the relative docs that should have been retrieved for the mQuery (basically just an exact line read from qRel)

    private List<String> mRetrievedRelevant; // list of only rel doc names that were retrieved from

    private List<String> mTotalRetrieved; // list of all doc names mQuery's getPostings()

    private List<String> mTotal; // list of all doc names in the entire corpus

    public EvaluatedQuery() {}

    public EvaluatedQuery(RankedQuery q) {
        mQuery = q;
        List<Posting> queryResults = q.getPostingsWithoutPositions()
}
