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
import java.util.*;

import com.google.common.base.Stopwatch;
import edu.csulb.Driver;
import edu.csulb.Driver.WeighingScheme;

import javax.management.Query;
import java.io.File;

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
            qString = Files.readAllLines(mQueryPath).get(lineNum - 1);
        } catch (IOException e) {
            System.out.println("Error: The query could not be read because the requested line number is past the end of the file.");
            e.printStackTrace();
        }
        mQuery = mParser.parseQuery(qString);
        try {
            String qRelString = Files.readAllLines(mQueryRelPath).get(lineNum - 1);
                List<String> qRels = Arrays.asList(qRelString.split(" "));
                mEvalQuery = new EvaluatedQuery(mQuery, qRels);
            }
        catch (IOException e) {
                e.printStackTrace();
        }

        return mEvalQuery;
    }

    public List<EvaluatedQuery> readBulkFileQueries(int count, boolean read_all) {
        List<EvaluatedQuery> results = new ArrayList<>();

        try {
            List<String> allQueries = Files.readAllLines(mQueryPath);

            if (read_all) {
                count = allQueries.size();
            }

            for (int i = 1; i < count + 1; i++) {
//                String qString = allQueries.get(i);
//                mQuery = mParser.parseQuery(qString);
//                EvaluatedQuery q = new EvaluatedQuery(mQuery);
                EvaluatedQuery q = evaluateFileQuery(i);
                results.add(q);
            }
        }
        catch (IOException e) {
            System.out.println("Error: The query could not be read because the requested line number is past the end of the file.");
            e.printStackTrace();
        }

        return results;
    }

    public QueryComponent readFileQuery(int doc) {
        QueryComponent q = mParser.parseQuery("");
        try {
            String qString = Files.readAllLines(mQueryPath).get(doc - 1);
            q = mParser.parseQuery(qString);
        }
        catch (IOException e) {
            System.out.println("Error: The query could not be read because the requested line number is past the end of the file.");
            e.printStackTrace();
        }

        return q;
    }



    public void useUserQuery(String input) {
        mQuery = mParser.parseQuery(input);
    }

    // calculates the MAP of all queries on file over the entire active corpus using configurable weighing scheme that is passed in
    // here 'k' represents the number of ranked postings that will be retrieved by the rankedQuery for the given calculation
    public double calculateAvgPrecision(WeighingScheme scheme, int kTerms, EvaluatedQuery q) {
        mEvalQuery = q;
        Driver.ActiveConfiguration.setWeightingScheme(scheme);
        int totalRel = mEvalQuery.getTotalRelevant().size(); // |Rel|
        HashMap<Integer, Boolean> relevance = mEvalQuery.getRetrievedRelevant(kTerms);
        double result = 0;

        // keep running totals for k and rel (where k = total retrieved docs at k and rel = total relevant docs at k
        int k = 0;
        int rel = 0;
        for (int doc : relevance.keySet()) {

            // first increment k for each retrieved doc, but only increment rel if the doc is relevant
            k += 1;
            if (relevance.get(doc)) {
                rel += 1;
            }

            // only calculate pAk at each step after appropriately incrementing k and rel
            double pAk = (double) rel / k;
            result += pAk;
        }

        // after adding up all  pAk values, divide by the total number of retrieved docs (|Rel|)
        result = result / totalRel;
        return result;
    }

    public double calculateResponseTime(QueryComponent q) {
        // start timer
        long start = System.currentTimeMillis();
        List<Posting> temp = q.getPostingsWithoutPositions(mProcessor, activeIndex);
        long stop = System.currentTimeMillis();

        return ((stop - start) / 1000.0);
    }

    public double calculateThroughput(QueryComponent q, int iterations) {
        double meanAvgResponse = 0.0;

        for (int i = 0; i < iterations; i++) {
            meanAvgResponse += calculateResponseTime(q);
        }

        meanAvgResponse = meanAvgResponse / iterations;
        return (1 / meanAvgResponse);

    }

    public double calculateMeanAvgPrecision(WeighingScheme scheme, int k) {
        double result = 0;
        List<EvaluatedQuery> allQueries = readBulkFileQueries(0, true);
        Driver.ActiveConfiguration.setWeightingScheme(scheme);

        for (EvaluatedQuery q : allQueries) {

            result += calculateAvgPrecision(scheme, k, q);
        }

        return (result / allQueries.size());
    }


}
