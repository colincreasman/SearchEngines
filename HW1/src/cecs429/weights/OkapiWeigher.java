package cecs429.weights;

import javax.print.Doc;
import static edu.csulb.Driver.ActiveConfiguration.activeCorpus;
import static edu.csulb.Driver.ActiveConfiguration.indexDao;

public class OkapiWeigher extends WeighingStrategy {

    // Okapi w(d,t) = [2.2 * tf(t,d)] / [(1.2 * (0.25 + 0.75 * (docLength(d)/docLength(A))]
    @Override
    public double calculateWdt(DocTermWeight w) {
        int docId = w.getDocId();
        double tfTd = w.getTermFrequency();
        long currDocLength = indexDao.readDocLength(docId);
        int avgDocLength = indexDao.readAvgDocLength();

        double numerator = 2.2 * tfTd;
        // make sure the avgDocLength isn't zero
        if (avgDocLength == 0) {
            avgDocLength += 1;
        }
        double denominator = ( (1.2 * (0.25 + 0.75 * (currDocLength / avgDocLength) ) ) );

        return numerator / denominator;
    }

    // Okapi w(q,t) = max[0.1, ln( [N - df(t) + 0.5] / [df(t) + 0.5]
    @Override
    public double calculateWqt(QueryTermWeight w) {
        int n = activeCorpus.getCorpusSize();
        int dFt = w.getDft();

        double fraction = (n - dFt + 0.5) / (dFt + 0.5);
        fraction = Math.log(fraction);

        return (Math.max(0.1, fraction));
    }

    // Okapi Ld = 1
    @Override
    public double calculateLd(DocWeight w) {
        return 1;
    }

    @Override
    public double readLd(DocWeight w) {
        return 1;
    }

}
