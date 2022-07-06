package Engine.Evaluations;

import Engine.Indexes.Posting;
import Engine.Queries.QueryComponent;
import Engine.Queries.RankedQuery;
import Engine.Text.AdvancedTokenProcessor;
import Engine.Text.TokenProcessor;

import java.util.List;

import static App.Driver.ActiveConfiguration.activeIndex;

// TODO: implement this so that it encapsulates a single RankedQuery object and a matching List<Integer> of relevant docId's
public class EvaluatedQuery {
    private TokenProcessor mProcessor;

    private RankedQuery mQuery;

    private List<String> mTotalRelevant; // list of all the relative docs that should have been retrieved for the mQuery (basically just an exact line read from qRel)
    private List<String> mTotalRetrieved; // list of all doc names mQuery's getPostings()

    private List<String> mRetrievedRelevant; // list of only rel doc names that were retrieved from

    private List<String> mTotal; // list of all doc names in the entire corpus

    public EvaluatedQuery() {}

    public EvaluatedQuery(QueryComponent q, List<String> qRel) {
        mProcessor = new AdvancedTokenProcessor();
//        List<Posting> qPostings = q.getPostingsWithoutPositions(mProcessor, activeIndex);
        mQuery = (RankedQuery) q;
    }


    public void setKterms(int k) {
        mQuery.setKterms(k);
    }
}
//        String unsplitQuery = mQuery
//        List<Posting> queryResults = q.getPostingsWithoutPositions()
//}
