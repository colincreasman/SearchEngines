package Engine.Queries;
import java.util.List;

import Engine.Indexes.Index;
import Engine.Indexes.Posting;
import cecs429.indexes.*;
import Engine.Text.TokenProcessor;

/**
 * A QueryComponent is one piece of a larger query, whether that piece is a literal string or represents a merging of
 * other components. All nodes in a query parse tree are QueryComponent objects.
 */
public interface QueryComponent {
    /**
     * Retrieves a list of postings for the query component, using an Index as the source.
     */
    List<Posting> getPostings(TokenProcessor processor, Index index);

    List<String> getProcessedTerms();

    List<Posting> getPostingsWithoutPositions(TokenProcessor processor, Index activeIndex);

//    HashMap<String, List<Posting>> getTermPostings();
}