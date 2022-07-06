package Engine.Weights;

import Engine.DataAccess.DbFileDao;
import Engine.DataAccess.FileDao;

import java.util.List;

import static App.Driver.ActiveConfiguration.activeCorpus;


public class DefaultWeigher extends WeighingStrategy {

    // Default w(d,t) = 1 + ln(tf(t,d))
    @Override
    public double calculateWdt(DocTermWeight w) {
        return (1 + Math.log(w.getTermFrequency()));
    }

    // Default w(q,t) = 1  + ln(1 + N/Dft)
    @Override
    public double calculateWqt(QueryTermWeight w) {
        double fraction = activeCorpus.getCorpusSize();/// w.docFrequency;
        return (1 + Math.log(fraction));
    }

    // Default Ld = sqrt(sum[w(d,t)^2])
    public double calculateLd(DocWeight w) {
        // get the list of w(d,t) values from the docWeight passed in
        List<DocTermWeight> wDts = w.getTermWeights();
        double sum = 0;

        for (DocTermWeight wDt : wDts ) {
            double dubLog = Math.log(wDt.getValue());
            dubLog = Math.pow(dubLog, 2.0);
            double basicWeight = 1 + dubLog; //w(t,d) = 1 + ln(tf(t,d))
            sum += basicWeight;
        }
        return Math.sqrt(sum);
    }

    @Override
    public double readWdt(DocTermWeight w) {
        double wDt = 0.0;
        mDbDao.open("termLocations");
        long byteLocation = mDbDao.readTermLocation(w.getTerm());

        // offset by (12 * docId + 4) to get to the weights of each term in the postings.binFile
        int weightIncrement = (12 * w.getDocId()) + 4;
        // the default weight is written first, so there is no additional offset from the scheme
        byteLocation += weightIncrement;
        mBinDao.open("postings");
        try {
            wDt = mBinDao.readDouble(byteLocation);
        }
        catch (Exception ex)   {
            System.out.println("Failed to read the wDt value from disk for term: " + w.getTerm());
        }
//        mBinDao.close("postings");
        return wDt;
    }

    @Override
    public double readLd(DocWeight w) {
        double weight = 0.0;
        try {
            int byteLocation = (w.getDocId() * 28); // 28 bytes used up for the 4 values written for each doc
            // the default weighing strategy uses the normal docWeights(d) formula, this should be the first value written for each docId in the file
            weight = mBinDao.readDouble(byteLocation);
        }
        catch (Exception ex) {
            System.out.println("Failed to read the doc weight from disk for docId: " + w.getDocId());
            ex.printStackTrace();
        }
        return weight;
    }


}
