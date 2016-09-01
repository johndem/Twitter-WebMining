
public class WebMiner {	//	general class(containing the mongo database) which most other classes inherit in order to use mongo
	
	protected MongoDB mongo;
	
	public WebMiner() {
		mongo = new MongoDB();
	}

}
