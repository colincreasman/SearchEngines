package Engine.Weights;

import Engine.DataAccess.FileDao;

import java.util.List;

import static App.Driver.ActiveConfiguration.activeCorpus;


public class DefaultWeigher extends WeighingStrategy {
    private FileDao termReader = new DbFileDao(); // used to read the byte location of a requested term

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
        List<DocTermWeight> wDts = w.getTermFrequencies();
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
    public double readWdt(Weight w, FileDao dao) {
        Weight wv = (Weight) dao.read(w);

        String term = w.getTerm();


        return 0;
    }


}
