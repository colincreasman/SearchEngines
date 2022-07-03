package cecs429.weights;

import cecs429.documents.Document;

import static edu.csulb.Driver.ActiveConfiguration.activeCorpus;

public class DocTermWeight implements Weight{
   // private String mTerm;
    private double mValue;
    //private WeighingStrategy mWeigher;
    private double mDocId;
    private Document mDocument;
    private int mTermFrequency; // tf(t,d)

    public DocTermWeight() { };

    public DocTermWeight(int docId, int termFrequency) {
        mDocId = docId;
        mDocument = activeCorpus.getDocument(docId);
     //   mWeigher = activeWeigher;
        mTermFrequency = termFrequency;
    }

    @Override
    public double calculate(WeighingStrategy weigher) {

        mValue = weigher.calculateWdt(this);
    }

    @Override
    public void read(WeighingStrategy weigher) {
        mValue = weigher.readWdt(this);
    }

    @Override
    public void write(WeighingStrategy weigher) {
        weigher.writeWdt(this);
    };

    public int getTermFrequency() {
        return mTermFrequency;
    }

    public void setTermFrequency(int termFrequency) {
         mTermFrequency = termFrequency;
    }

    public double getValue() {
        return mValue;
    }

//    public String getTerm() {
//        return mTerm;
//    }

    public Document getDocument() {
        return mDocument;
    }

//    @Override
//    public int compareTo(@NotNull DocTermWeight w) {
//        int result = -2;
//        try {
//            if (Objects.equals(mTerm, w.getTerm())) {
//                if (mDocument == w.getDocument()) {
//                    result = 0;
//                } else if (mDocument.getId() > w.getDocument().getId()) {
//                    result = -1;
//                } else {
//                    result = 1;
//                }
//            }
//        }
//        catch (Exception ex) {
//            System.out.println("Error: The DocTermWeights cannot be compared because they do not reference the same term.");
//        }
//        return result;
//    }
}
