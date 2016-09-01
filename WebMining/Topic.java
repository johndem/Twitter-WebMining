import java.util.HashSet;

public class Topic {	//	represents a temporary event/topic detected
	
	private String id;	//	topic's id/keywords
	private HashSet<String> docs;	//	list of doc/tweet ids containing the topic's keywords
	
	public Topic() {
		docs = new HashSet<String>();
	}
	
	public Topic(String id, HashSet<String> docs) {
		this.id = id;
		this.docs = docs;
	}
	
	public String getId() {
		return id;
	}
	
	public HashSet<String> getDocs() {
		return docs;
	}
	
	public void insertDoc(String doc) {
		docs.add(doc);
	}
	
	public int getListSize() {
		return docs.size();
	}
	
	public void setDocs(HashSet<String> docs) {
		this.docs = docs;
	}
	
	public void setId(String id) {
		this.id = id;
	}

}
