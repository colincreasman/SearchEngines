package cecs429.weights;

import edu.csulb.Driver.WeighingScheme;
import edu.csulb.Driver.ActiveConfiguration;
import cecs429.indexes.*;
import cecs429.documents.*;
import static edu.csulb.Driver.ActiveConfiguration.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.RunMode.BUILD;

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


    // cannot read wQt vals since they are never written to disk
    @Override
    public void read(WeighingScheme scheme) {
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

    public int getN() {
        return mCorpusSize;
    }

    public int getDft() {
        return mDocFrequency;
    }

}