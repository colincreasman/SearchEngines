package cecs429.text;

public class DocWeight implements Weight {
    private WeightCalculator mCalculator;
    private double mValue;

    public DocWeight(WeightCalculator calculator) {
        mCalculator = calculator;
    }

    @Override
    public void calculate() {
        mValue = mCalculator.calculateLd();
    }

    @Override
    public void write() {

    }
}
