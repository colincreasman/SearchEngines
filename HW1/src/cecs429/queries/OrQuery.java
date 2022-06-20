package cecs429.queries;

import java.util.*;
import java.util.stream.Collectors;

import cecs429.indexes.Index;
import cecs429.indexes.Posting;
import cecs429.text.AdvancedTokenProcessor;
import cecs429.text.TokenProcessor;

/**
 * An OrQuery composes other QueryComponents and merges their postings with a union-type operation.
 */
public class OrQuery implements QueryComponent {
	// The components of the Or query.
	private List<QueryComponent> mComponents;
	private static AdvancedTokenProcessor mProcessor;
	private static List<String> mProcessedTerms;
	
	public OrQuery(List<QueryComponent> components) {
		//mProcessor = processor;
		mComponents = components;
		mProcessedTerms = new ArrayList<>();

	}
	
	@Override
	public List<Posting> getPostings(TokenProcessor processor, Index index) {
		// initialize a master postings list to which we will add all of the individual lists from each component after any necessary processing
		List<Posting> masterPostingsList = new ArrayList<>();

		// process tokens and normalize before searching the index
		for (QueryComponent c : mComponents) {
			List<String> currentTerms = processor.processToken(c.toString());
			mProcessedTerms.addAll(currentTerms);
		}
		//Collections.sort(mProcessedTerms);

		try {
			for (int i = 0; i < mProcessedTerms.size(); i++) {
				// for the first QueryComponent only, automatically add it to the master postings list without any OR processing
				if (i == 0) {
					masterPostingsList.addAll(index.getPostings(mProcessedTerms.get(i - 1)));
				}
				// for every other query component after that, update the master list by OR-ing it with the current component's postings list
				else {
					masterPostingsList = union(index.getPostings(mProcessedTerms.get(i - 1)), masterPostingsList);
				}
			}
			return masterPostingsList;
		}

		catch (Exception ex) {
			System.out.println("No documents were found containing the OR query '" + this + "'");
			return null;
		}
	}

	// performs the OR union merge of two lists of postings to return a new list containing all postings from both lists (without duplicates)
	public List<Posting> union(List<Posting> top, List<Posting> bottom) {
		// initialize list to store results
		List<Posting> results = new ArrayList<>();

		// set up indexes to iterate through both postings lists simultaneously
		int i = 0; // top
		int j = 0; // bottom

		// iterate through both lists simultaneously
		while (i < top.size() && j < bottom.size()) {
			// find the documentId's of each list at their respective current indexes
			int topDocId = top.get(i).getDocumentId();
			int bottomDocId = bottom.get(j).getDocumentId();

			// if they are equal, add either list's current posting (top chosen arbitrarily here) to the results and increment both indexes
			if (topDocId == bottomDocId) {
				Posting merge = top.get(i).merge(bottom.get(j));
				results.add(merge);
				i++;
				j++;
			}

			// otherwise, add only the posting from the list whose current docId is smaller than the other's
			else if (topDocId < bottomDocId) {
				results.add(top.get(i));
				// then increment this list's index to catch up with the other one
				i++;
			}
			else if (bottomDocId < topDocId) {
				results.add(bottom.get(j));
				j++;
			}
		}

		// account for lists with different sizes by continuing to iterate to the end of each list and adding any leftover postings to the results lists
		while (i < top.size()) {
			// add the remaining posting(s)
			Posting remaining = top.get(i++);
			results.add(remaining);
		}
		while (j < bottom.size()) {
			Posting remaining = bottom.get(j++);
			results.add(remaining);
		}
		return results;
	}
	
	@Override
	public String toString() {
		// Returns a string of the form "[SUBQUERY] + [SUBQUERY] + [SUBQUERY]"
		return "Query components: (" +
		 String.join(" + ", mComponents.stream().map(c -> c.toString()).collect(Collectors.toList()) + "\n Processed terms: (" + String.join(" + ", mProcessedTerms.stream().map(c -> c.toString()).collect(Collectors.toList())));
	}
}
