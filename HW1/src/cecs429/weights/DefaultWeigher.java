package cecs429.weights;

import java.util.List;
import static edu.csulb.Driver.ActiveConfiguration.activeCorpus;
import static edu.csulb.Driver.ActiveConfiguration.indexDao;

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
    public double readLd(DocWeight w) {
        double weight = 0.0;
        int byteLocation = (w.getDocId() * 28); // 28 bytes used up for the 4 values written for each doc
            // the default weighing strategy uses the normal docWeights(d) formula, this should be the first value written for each docId in the file
        weight = indexDao.readDocWeight(byteLocation);
        return weight;
    }
}