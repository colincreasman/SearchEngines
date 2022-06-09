package cecs429.queries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;

/**
 * Represents a phrase literal consisting of one or more terms that must occur in sequence.
 */
public class PhraseLiteral implements QueryComponent {
	// The list of individual terms in the phrase.
	private List<String> mTerms;
	private AdvancedTokenProcessor mProcessor;

	/**
	 * Constructs a PhraseLiteral with the given individual phrase terms.
	 */
	public PhraseLiteral(List<String> terms) {
		mProcessor = new AdvancedTokenProcessor();
		mTerms = new ArrayList<>();
		// before getting postings, each term in the list must be processed (without removing hyphens)
		for (String t : terms) {
			String processedTerm = mProcessor.processTokenWithHyphens(t);
			mTerms.add(processedTerm);
		}
	}
	
	/**
	 * Constructs a PhraseLiteral given a string with one or more individual terms separated by spaces.
	 */
	public PhraseLiteral(String terms) {
		mTerms.addAll(Arrays.asList(terms.split(" ")));
	}
	
	@Override
	public List<Posting> getPostings(Index index) {
		// initialize master list to hold all of the postings that pass each positional merge
		List<Posting> masterList = index.getPostings(mTerms.get(0));

		// loop through the remaining terms in the phrase and perform the positionalMerge
		for (int i = 1; i < mTerms.size(); i++) {
			// update the master list by positionally merging it with the current postings list
			List<Posting> currentPostings = index.getPostings(mTerms.get(i));

			try {
				masterList = positionalMerge(currentPostings, masterList);
			}
			catch (NullPointerException ex) {
				System.out.println("No documents were found containing the phrase literal '" + this + "'");
				masterList = null;
			}
		}
		return masterList;
	}

	/**
	 * Given two postings lists that each corresponds to a term (or group of terms) from the list of mTerms, finds the postings of documents that contain both of them together in order
	 * @param top represents the master "top"-level that holds all the postings of terms that have already been positionally merged together
	 * @param bottom represents the new "bottom"-level list of postings for the new term that is being merged with the master
	 * @return a new list containing only the postings that survived the merge
	 */
	public List<Posting> positionalMerge(List<Posting> top, List<Posting> bottom) {
		// initialize results list
		List<Posting> mergedPostings = new ArrayList<>();
		// setup indexes for iterating both lists simultaneously
		int topIndex = 0;
		int bottomIndex = 0;

		while (topIndex < top.size() && bottomIndex < bottom.size()) {

			// check if the current docIds match before proceeding
			if (top.get(topIndex).getDocumentId() == bottom.get(bottomIndex).getDocumentId()) {

				// use to store the positions of matching term positions
				List<Integer> matches = new ArrayList<>();
				// use to store the term positions at the current index in the top and bottom lists
				List<Integer> topTermPositions = top.get(topIndex).getTermPositions();
				List<Integer> bottomTermPositions = bottom.get(bottomIndex).getTermPositions();

				// starting at current top-level index of each list, iterate through their lists of the term positions using new indexes
				int topTermIndex = 0;
				int bottomTermIndex = 0;
				while (topTermIndex < topTermPositions.size() && bottomTermIndex < bottomTermPositions.size()) {
					// now we must handle the two possible comparison cases

					// the first case occurs if the current term positions in  each list are equal
					if (topTermPositions.get(topTermIndex) == bottomTermPositions.get(bottomTermIndex)) {
						// if so, add the position to the list of matched positions and increment both terms indexes
						matches.add(topTermPositions.get(topTermIndex));
						topTermIndex++;
						bottomTermIndex++;
					}

					// the other case occurs when the current term positions in  each list are not equal
					// this is handled by simply incrementing the term index of the list with the lower term position
					else if (topTermPositions.get(topTermIndex) > bottomTermPositions.get(bottomTermIndex)) {
						bottomTermIndex++;
					} else {
						topTermIndex++;
					}
				}
				// increment both top-level indexes to continue iterating
				topIndex++;
				bottomIndex++;
			}

			// if the docId's aren't equal, just increment to top-level index of whichever list has the lower docId
			else if (top.get(topIndex).getDocumentId() > bottom.get(bottomIndex).getDocumentId()) {
				bottomIndex++;
			} else {
				topIndex++;
			}
			// end of a single iteration of top-level while loop
		}
		return mergedPostings;
	}
	
	@Override
	public String toString() {
		return "\"" + String.join(" ", mTerms) + "\"";
	}
}
