package Engine.Evaluations;


import App.Driver.WeighingScheme;
import Engine.DataAccess.FileDao;
import Engine.DataAccess.TxtFileDao;
import Engine.Indexes.DiskPositionalIndex;
import Engine.Indexes.Posting;
import Engine.Queries.QueryComponent;
import Engine.Queries.QueryParser;
import Engine.Queries.RankedQuery;
import Engine.Queries.RankedQueryParser;
import Engine.Text.AdvancedTokenProcessor;
import Engine.Text.TokenProcessor;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static App.Driver.ActiveConfiguration.*;

// driver for the entire evaluation service
public class Evaluator {
    private File mRelevanceDir;
    private FileDao mQueryDao;
    private RankedQueryParser mParser = new RankedQueryParser();
    TokenProcessor mProcessor;
    private QueryComponent mFileQuery;
    private EvaluatedQuery mEvalQuery;


    public Evaluator() {
        String relPath = activeCorpus.getPath() + "/relevance";
        mRelevanceDir = new File(relPath);
        mQueryDao = new TxtFileDao(mRelevanceDir);
        mParser = new RankedQueryParser();
        mProcessor = new AdvancedTokenProcessor();
    }

    // takes in the int of a line number to read from file to find the query to be evaluated
    public void useFileQuery(int lineNum) {
        mQueryDao.open("queries");
        String qString = mQueryDao.readSingleLine(lineNum);
        mFileQuery = mParser.parseQuery(qString);
        mQueryDao.close("queries");

        mQueryDao.open("qRel");
        String qRelString = mQueryDao.readSingleLine(lineNum);
        List<String> qRels = Arrays.asList(qRelString.split(" "));
        mEvalQuery = new EvaluatedQuery(mFileQuery, qRels);
        mQueryDao.close("qRel");
    }

    public void useUserQuery(String input) {
        mFileQuery = mParser.parseQuery(input);
    }

    // calculates the MAP of all queries on file over the entire active corpus using configurable weighing scheme weighing scheme that is passed in
    // here 'k' represents the number of ranked postings that will be retrieved by the rankedQuery for the given calculation
    public void evaluateApQ(WeighingScheme scheme, int k) {
        activeWeighingScheme = scheme;
        mEvalQuery.setKterms(k);
    }

    public void evaluateMapQ(WeighingScheme scheme, int k) {
        activeWeighingScheme = scheme;

    }


    }
