package Engine.Queries;

import java.util.*;
import java.util.stream.Collectors;

import Engine.Indexes.Index;
import Engine.Indexes.Posting;
import Engine.Text.TokenProcessor;

/**
 * An AndQuery composes other QueryComponents and merges their postings in an intersection-like operation.
 * An AndQuery occurs when there are more than one consecutive QueryComponents that do not have the '+' between them
 */
public class AndQuery implements QueryComponent {
	private final List<QueryComponent> mComponents;
	private static List<String> mProcessedTerms;
//	private static HashSet<Integer> mDocIds;
//	private static List<Posting> mResults;

	public AndQuery(List<QueryComponent> components) {
		mComponents = components;
//		mDocIds = new HashSet<>();
//		mResults = new ArrayList<>();
		mProcessedTerms = new ArrayList<>();

	}

	
	@Override
	public List<Posting> getPostings(TokenProcessor processor, Index index) {
		// process and normalize tokens into terms to use to search the index
		for (QueryComponent c : mComponents) {
			List<String> currentTerms = processor.processToken(c.toString());
			mProcessedTerms.addAll(currentTerms);
		}
		//Collections.sort(mProcessedTerms);

		// initialize a master postings list with the postings for the first processed term in the list
		List<Posting> masterPostingsList = index.getPostings(mProcessedTerms.get(0));

		//mResults = mComponents.get(0).getPostings(index);
		try {
			for (int i = 1; i < mProcessedTerms.size(); i++) {
				masterPostingsList = intersect(masterPostingsList, index.getPostings(mProcessedTerms.get(i)));
			}
			return masterPostingsList;
		}
		catch (Exception ex) {
			System.out.println("No documents were found containing the AND query '" + this + "'");
			return null;
		}
	}

	@Override
	public List<String> getProcessedTerms() {
		return null;
	}

	@Override
	public List<Posting> getPostingsWithoutPositions(TokenProcessor processor, Index activeIndex) {
		return null;
	}

	// performs an AND intersect merge of two lists of postings to return a new list containing only the postings the are found in both of the original lists
	public List<Posting> intersect(List<Posting> top, List<Posting> bottom) {

		// initialize the results list
		List<Posting> results = new ArrayList<>();

		// set up indexes to iterate through both postings lists simultaneously
		int i = 0; // top index
		int j = 0; // bottom index

		// iterate through both lists simultaneously
		while (i < top.size() && j < bottom.size()) {
			// find the documentId's of each list at their respective current indexes
			int topDocId = top.get(i).getDocumentId();
			int bottomDocId = bottom.get(j).getDocumentId();

			// if they are equal, merge the two postings and add to the results
			if (topDocId == bottomDocId) {
				Posting match = (top.get(i).merge(bottom.get(j)));
				Collections.sort(match.getTermPositions());
				results.add(match);
				i++;
				j++;
			}
			// if the current docId's aren't equal, increment the one with the lower docId
			else if (topDocId < bottomDocId) {
				i++;
			}
			else {
				j++;
			}
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
