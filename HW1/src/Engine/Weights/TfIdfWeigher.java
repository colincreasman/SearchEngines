package Engine.Weights;

import java.util.List;

public class TfIdfWeigher extends WeighingStrategy {

    // TF_IDF w(d,t) = tf(t,d)
    @Override
    public double calculateWdt(DocTermWeight w) {
        return w.getTermFrequency();
    }

    // TF_IDF w(q,t) = idf(t) = ln(N/df(t))
    @Override
    public double calculateWqt(QueryTermWeight w) {
        int n = w.getN();
        int dFt = w.getDft();
        double idFt = Math.log(n / dFt);
        return idFt;
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
}
