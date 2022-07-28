package cecs429.weights;

import edu.csulb.Driver.WeighingScheme;
import edu.csulb.Driver.ActiveConfiguration;
import cecs429.indexes.*;
import cecs429.documents.*;
import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.ActiveConfiguration.activeCorpus;

public class WackyWeigher extends WeighingStrategy {

    // Wacky w(d,t) = [1 + ln(tf(t,d))] / [1 + ln(avg(tf(t,d)))]
    @Override
    public double calculateWdt(DocTermWeight w) {
        int docId = w.getDocId();
        int tfTd = w.getTermFrequency();
        int avgTfTd = indexDao.readAvgTermFrequency(docId);

        double numerator = 1 + Math.log(tfTd);
        double denominator = 1 + Math.log(avgTfTd);

        return numerator / denominator;
    }

    // Wacky w(q,t) = max[ 0, ln((N - df(t)) / df(t)) ]
    @Override
    public double calculateWqt(QueryTermWeight w) {
        int n = activeCorpus.getCorpusSize();
        int dFt = w.getDft();
        double fraction = ( Math.log(n - dFt) ) / dFt;

        return Math.max(0, fraction);
    }

    // Wacky Ld = sqrt(byteSize(d))
    public double calculateLd(DocWeight w) {
        int docId = w.getDocId();
        long byteSize = indexDao.readByteSize(docId);

        return Math.sqrt(docId);
    }

}
