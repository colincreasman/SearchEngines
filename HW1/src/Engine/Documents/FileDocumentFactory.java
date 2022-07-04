package Engine.Documents;

import java.nio.file.Path;

public interface FileDocumentFactory {
	FileDocument createFileDocument(Path absoluteFilePath, int documentId);
}
