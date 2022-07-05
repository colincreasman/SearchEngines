package Engine.Weights;

import App.Driver;
import App.Driver.WeighingScheme;
import Engine.Documents.Document;
import Engine.Indexes.Posting;

import static App.Driver.ActiveConfiguration.activeCorpus;
import static App.Driver.ActiveConfiguration.activeWeighingScheme;

public class QueryTermWeight implements Weight {
    private String mTerm;
    private double mValue;
    private WeighingStrategy mWeigher;
    private Document mDocument;
    private int mDocFrequency; // Dft
    private int mCorpusSize = activeCorpus.getCorpusSize();


    public QueryTermWeight() { };

    public QueryTermWeight(String term, int docFrequency) {
        mDocFrequency = docFrequency;
        mTerm = term;
    }

    @Override
    public void calculate(WeighingScheme scheme) {
        mWeigher = scheme.getInstance();
        mValue = mWeigher.calculateWqt(this);
    }

    @Override
    public void read(WeighingScheme scheme) {
            mWeigher = scheme.getInstance();
            mValue = mWeigher.readWqt(this);
    }

    public double getValue() {
        // if the value hasn't been calculated yet, call calculate now with the current active scheme
        if (mValue == 0) {
            calculate(activeWeighingScheme);
        }
        return mValue;
    }

    public Document getDocument() {
        return mDocument;
    }

    public String getTerm() {
        return mTerm;
    }

}
