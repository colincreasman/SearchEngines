package cecs429.weights;

public class TfIdfWeigher extends WeighingStrategy {

    // TF_IDF w(d,t) = tf(t,d)
    @Override
    public double calculateWdt(DocTermWeight w) {
        return 0;
    }

    // TF_IDF w(q,t) = idf(t) = ln(N/df(t))
    @Override
    public double calculateWqt(QueryTermWeight w) {
        return 0;
    }

    // TF_IDF Ld = sqrt(sum[w(d,t)^2])
    @Override
    public double calculateLd(DocWeight w) {
        return 0;
    }
}
