package cecs429.evaluation;

import cecs429.indexes.Posting;
import cecs429.queries.QueryComponent;
import cecs429.queries.RankedQuery;
import cecs429.queries.RankedQueryParser;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.TokenProcessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import edu.csulb.Driver.WeighingScheme;
import java.io.File;
import java.util.Arrays;

import static edu.csulb.Driver.ActiveConfiguration.*;


// driver for the entire evaluation service
public class Evaluator {
    private String mRelevancePath;
    private Path mQueryPath;
    private Path mQueryRelPath;

    private RankedQueryParser mParser = new RankedQueryParser();
    TokenProcessor mProcessor;
    private QueryComponent mQuery;
    private EvaluatedQuery mEvalQuery;


    public Evaluator() {
        mRelevancePath = activeCorpus.getPath() + "/relevance";
        mQueryPath = Paths.get(mRelevancePath + "/queries").toAbsolutePath();
        mQueryRelPath = Paths.get(mRelevancePath + "/qrel").toAbsolutePath();
        mParser = new RankedQueryParser();
        mProcessor = new AdvancedTokenProcessor();
    }

    // takes in the int of a line number to read from file to find the query to be evaluated
    public EvaluatedQuery evaluateFileQuery(int lineNum) {
        String qString = "";
        try {
            qString = Files.readAllLines(mQueryPath).get(lineNum);
        } catch (IOException e) {
            System.out.println("Error: The query could not be read because the requested line number is past the end of the file.");
            e.printStackTrace();
        }
        mQuery = mParser.parseQuery(qString);
        try {
            String qRelString = Files.readAllLines(mQueryRelPath).get(lineNum);
                List<String> qRels = Arrays.asList(qRelString.split(" "));
                mEvalQuery = new EvaluatedQuery(mQuery, qRels);
            }
        catch (IOException e) {
                e.printStackTrace();
        }
        return mEvalQuery;
    }

    public void useUserQuery(String input) {
        mQuery = mParser.parseQuery(input);
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
