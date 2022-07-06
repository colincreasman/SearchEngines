package Engine.Evaluations;


import Engine.DataAccess.FileDao;
import Engine.DataAccess.TxtFileDao;
import Engine.Indexes.DiskPositionalIndex;
import Engine.Indexes.Posting;
import Engine.Queries.QueryParser;
import Engine.Queries.RankedQuery;
import Engine.Queries.RankedQueryParser;
import Engine.Text.AdvancedTokenProcessor;
import Engine.Text.TokenProcessor;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.util.List;

import static App.Driver.ActiveConfiguration.activeCorpus;

// driver for the entire evaluation service
public class Evaluator {
    private File mRelevanceDir;
    private FileDao mQueryDao;
    private TokenProcessor mProcessor = new AdvancedTokenProcessor();
    private QueryParser mParser = new RankedQueryParser();
    private RankedQuery mQuery;


    public Evaluator() {
        String relPath = activeCorpus.getPath() + "/relevance";
        mRelevanceDir = new File(relPath);
        mQueryDao = new TxtFileDao(mRelevanceDir);
        mParser = new RankedQueryParser();
        mProcessor = new AdvancedTokenProcessor();
    }

    public void evaluateFileQuery(int lineNum) {
        mQueryDao.open("qRel");

    }
    }
