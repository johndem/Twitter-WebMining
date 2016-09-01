package tweetstreamer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterObjectFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Main {

    public static void main(String[] args) throws IOException {

        File tweetDir = new File("./Tweets");
        if (!tweetDir.isDirectory()) {
            tweetDir.mkdir();
        }

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true);
        cb.setJSONStoreEnabled(true);
        cb.setOAuthConsumerKey("EOpZDp4Y7DLwScGKin2TW0aVb");
        cb.setOAuthConsumerSecret("FUMb18eV2JBBEkkgcksV1VGwlWXCDP1uy9PcKubqePWzoepDP1");
        cb.setOAuthAccessToken("2829735517-tq23K50NDZZc4PRpAgiGKZk0opqDqM45iK03N2D");
        cb.setOAuthAccessTokenSecret("qlfg86qpAknOoVtvBiOMHQOsQFh9voJNCV6kh37K0LQ9T");

        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
                String json = TwitterObjectFactory.getRawJSON(status);
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter("./Tweets/" + status.getId() + ".json"));
                    bw.write(json);
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
            }
        };

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        String keywords = br.readLine();
        br.close();

        FilterQuery fq = new FilterQuery();
        fq.track(keywords.split(","));

        twitterStream.addListener(listener);
        twitterStream.filter(fq);
    }
}
