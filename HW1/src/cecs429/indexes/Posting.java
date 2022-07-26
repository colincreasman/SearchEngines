package cecs429.indexes;

import cecs429.weights.DocTermWeight;
import cecs429.weights.DocWeight;
import cecs429.weights.QueryTermWeight;
import org.jetbrains.annotations.NotNull;
import static edu.csulb.Driver.ActiveConfiguration.*;
import static edu.csulb.Driver.QueryMode.RANKED;
import java.util.*;

/**
 * A Posting encapsulates a document ID associated a list of term positions within the document
 */
public class Posting implements Comparable<Posting> {
	private int mDocumentId;
	private List<Integer> mTermPositions;
	private int mTermFrequency; // tf(t,d)
	private DocTermWeight mDocTermWeight; // wdt
	private QueryTermWeight mQueryTermWeight; // wdt
	private  DocWeight mDocWeight; // lD
//	private double mAccumulator;  // Ad

	public Posting() {
		mTermPositions = new ArrayList<>();
//		mAccumulator = 0;
	}

	/**
	 * Simple constructor for making a posting with only a documentId at the time of initialization
	 * @param documentId
	 */
	public Posting(int documentId) {
		mDocumentId = documentId;
		mTermPositions = new ArrayList<>();
		mTermFrequency = 0;
//		mAccumulator = 0;
		mDocTermWeight = new DocTermWeight();
		mDocWeight = activeCorpus.getDocument(documentId).getWeight();
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
		mTermFrequency = mTermPositions.size();
//		mAccumulator = 0;
		mDocTermWeight = new DocTermWeight(documentId, mTermFrequency);
		mDocWeight = activeCorpus.getDocument(documentId).getWeight();
	}
	/**
	 * Overloaded constructor for making a posting with only alist of termPosition at the time of initialization
	 * @param documentId
	 * @param termPositions
	 */
	public Posting(int documentId, List<Integer> termPositions) {
		mDocumentId = documentId;
		mTermPositions = termPositions;
		mTermFrequency = mTermPositions.size();
//		mAccumulator = 0;
		mDocTermWeight = new DocTermWeight(documentId, mTermFrequency);
		//mDocWeight = activeCorpus.getDocument(documentId).getWeight();
	}

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
			result = "[w(q,t) = " + mQueryTermWeight.getValue() + "; w(d,t) = " + mDocTermWeight.getValue() + "; tf(t,d) = " + mDocTermWeight.getTermFrequency() + "]";
		}
		else {
			result = mDocumentId + ":" + mTermPositions.toString();
		}
		return result;

	}

	public int getTermFrequency() {
		return mTermFrequency;
	}

	public void setTermFrequency(int tfTd) {
		mTermFrequency = tfTd;
	}

	public DocTermWeight getDocTermWeight() {
		return mDocTermWeight;
	}

	public void setDocTermWeight(double w) {
		mDocTermWeight.setValue(w);
	}

	public DocWeight getDocWeight() {
		return mDocWeight;
	}

	public void setDocWeight(DocWeight w) {
		mDocWeight = w;
	}

	public QueryTermWeight getQueryTermWeight() {
		return mQueryTermWeight;
	}

	@Override
	public int compareTo(@NotNull Posting p) {
		return Integer.compare(mDocumentId, p.getDocumentId());
	}
}
