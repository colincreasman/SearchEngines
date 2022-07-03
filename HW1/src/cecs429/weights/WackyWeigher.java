package cecs429.weights;

public class WackyWeigher extends WeighingStrategy {


    // Wacky w(d,t) = [1 + ln(tf(t,d))] / [1 + ln(avg(tf(t,d)))]
    @Override
    public double calculateWdt(DocTermWeight w) {
        return (1 + Math.log(w.getTermFrequency()));
    }

    // Wacky w(q,t) = max[ 0, ln((N - df(t)) / df(t)) ]
    @Override
    public double calculateWqt(QueryTermWeight w) {
        return 0;
    }

    // Wacky Ld = sqrt(byteSize(d))
    public double calculateLd(DocWeight w) {
        return 0;
    }

}

