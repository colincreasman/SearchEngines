package Engine.Weights;

import static App.Driver.ActiveConfiguration.activeCorpus;

public class WackyWeigher extends WeighingStrategy {


    // Wacky w(d,t) = [1 + ln(tf(t,d))] / [1 + ln(avg(tf(t,d)))]
    @Override
    public double calculateWdt(DocTermWeight w) {
        DocWeight refLd = w.getDocument().getWeight();
        int tfTd = w.getTermFrequency();
        int avgTfTd = refLd.getAvgTermFrequency();

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
        return Math.sqrt(w.getByteSize());

    }

}

