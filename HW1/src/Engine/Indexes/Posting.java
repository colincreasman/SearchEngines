package Engine.Indexes;

import Engine.Weights.DocTermWeight;
import org.jetbrains.annotations.NotNull;

import static App.Driver.ActiveConfiguration.*;
import static App.Driver.QueryMode.RANKED;


import java.util.*;

/**
 * A Posting encapsulates a document ID associated a list of term positions within the document
 */
public class Posting implements Comparable<Posting> {
	private int mDocumentId;
	private List<Integer> mTermPositions;
	private int mTfTd; // tf(t,d)
	private DocTermWeight mDocTermWeight; // wdt
	private double mAccumulator;  // Ad

	public Posting() {
		mTermPositions = new ArrayList<>();
		mAccumulator = 0;
	}

	/**
	 * Simple constructor for making a posting with only a documentId at the time of initialization
	 * @param documentId
	 */
	public Posting(int documentId) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
		mTfTd = 0;
		mAccumulator = 0;
		mDocTermWeight = new DocTermWeight();
	}
	/**
	 * Overloaded constructor for making a posting with only a single termPosition at the time of initialization
	 * @param documentId
	 * @param termPosition
	 */
	public Posting(int documentId, int termPosition) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
		mTermPositions.add(termPosition);
		mTfTd = mTermPositions.size();
		mAccumulator = 0;
		mDocTermWeight = new DocTermWeight(documentId, mTfTd);
	}
	/**
	 * Overloaded constructor for making a posting with only alist of termPosition at the time of initialization
	 * @param documentId
	 * @param termPositions
	 */
	public Posting(int documentId, List<Integer> termPositions) {
		mDocumentId = documentId;
		mTermPositions = termPositions;
		mTfTd = mTermPositions.size();
		mAccumulator = 0;
		mDocTermWeight = new DocTermWeight(documentId, mTfTd);
	}
//	/**
//	 * Overloaded constructor for positionless postings in ranked retrieval
//	 * includes additional fields for doc/term weights and
//	 * @param documentId
//	 * @param termWeight
//	 * @param termFrequency
//	 */
//	public Posting(int documentId, double termWeight, int termFrequency) {
//		mDocumentId = documentId;
//		mDocTermWeight = termWeight;
//		mPositionsCount = termFrequency;
////		mDocWeight = docWeight;
//		mAccumulator = 0;
//	}

//	/**
//	 * Overloaded constructor for positionless postings in ranked retrieval
//	 * includes additional fields for doc/term weights and
//	 * @param documentId
//	 * @param termWeight
//	 * @param termFrequency
//	 */
//	public Posting(int documentId, double termWeight, int termFrequency, List<Integer> positions) {
//		mDocumentId = documentId;
//		mDocTermWeight = termWeight;
//		mPositionsCount = termFrequency;
//		mTermPositions = positions;
//		mAccumulator = 0;
//	}

	public void addTermPosition (int position) {
		mTermPositions.add(position);
		Collections.sort(mTermPositions);
		mDocTermWeight.setTermFrequency(mTermPositions.size());
	}

	public int getDocumentId() {
		return mDocumentId;
	}

	public List<Integer> getTermPositions() {
		return mTermPositions;
	}

	// given two Separate postings objects with the same docID's, returns a single Posting instance that maps their common docId to an aggregate list of  all the term positions from both original Postings (without duplicates)
	public Posting merge(Posting b) {
		///List<Integer> results = new	ArrayList<>();
		// return the original posting if the docId's are not the same
		if  (this.mDocumentId != b.mDocumentId) {
		//	System.out.println("Error: The term positions from the provided postings cannot be merged because they do not map to the same docID. Returning the original (unmerged) Posting\n" );
			return this;
		}
		else {
			// create a HashSet that will store all non-duplicated term positions from both postings
			HashSet<Integer> aggregate = new HashSet<>();
			// add all term positiongs to the set
			aggregate.addAll(this.mTermPositions);
			aggregate.addAll(b.mTermPositions);
			List<Integer> sharedPositions = new ArrayList<>();

			// now convert back into a list to allow Posting instantiation
			sharedPositions.addAll(aggregate);

			Posting results = new Posting(b.mDocumentId, sharedPositions);
			return results;
		}
	}

	// wraps a single posting string as "docId:[pos1,pos2,...,etc.]"
	@Override
	public String toString() {
		String result;
		if (queryMode == RANKED || mTermPositions == null) {
//			result = "[w(q,t) = " + mQueryTermWeight + "; w(d,t) = " + mDocTermWeight + "; tf(t,d) = " + mPositionsCount + "]";
			result = "[w(d,t) = " + mDocTermWeight + "; tf(t,d) = " + mTermPositions.size() + "]";

		}
		else {
			result = mDocumentId + ":" + mTermPositions.toString();
		}
		return result;

	}

	public int getTfTd() {
		return mTfTd;
	}

	public DocTermWeight getDocTermWeight() {
		return mDocTermWeight;
	}

	public void increaseAccumulator(double acc) {
		mAccumulator += acc;
	}

	public double getAccumulator() {
		return mAccumulator;
	}

	public void setAccumulator(double newAcc) {
		mAccumulator = newAcc;
	}

	@Override
	public int compareTo(@NotNull Posting p) {
		if (mAccumulator < p.getAccumulator()) {
			return -1;
		}
		else if (p.getAccumulator() < mAccumulator) {
			return 1;
		}
		else {
			return 0;
		}
	}

}

