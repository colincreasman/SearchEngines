package cecs429.text;

public abstract class WeightCalculator {

    private Weight mWeight; // maintain a reference to the concrete Context instance that is implementing this

    // each calculate method is made abstract to allow  Weight context being referenced to only implement its appropriate method(s)
    public abstract double calculateWdt();

    public abstract double calculateWqt();

    public abstract double calculateLd();


}
