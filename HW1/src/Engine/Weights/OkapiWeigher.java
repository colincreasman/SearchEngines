package Engine.Weights;

public class OkapiWeigher extends WeighingStrategy {

    // Okapi w(d,t) = max[0.1, ln( [N - df(t) + 0.5] / [df(t) + 0.5]
    @Override
    public double calculateWdt(DocTermWeight w) {
        return 0;
    }

    // Okapi w(q,t) = [2.2 * tf(t,d)] / [(1.2 * (0.25 + 0.75 * (docLength(d)/docLength(A))]
    @Override
    public double calculateWqt(QueryTermWeight w) {
        return 0;
    }

    // Okapi Ld = 1
    @Override
    public double calculateLd(DocWeight w) {
        return 1;
    }


}

