# Twitter-WebMining
Use the Twitter StreamingAPI to mine data and perform topic detection and affective analysis.

The project consists of the following phases and implementations:

1. Use the Twitter Streaming API to collect a large amount of data/tweets in JSON format.

2. Preprocess the JSON files and store the resulting data into MongoDB collections.

3. Perform topic detection in order to discover trending topics in the collected tweets by finding bursty keywords in set timeframes.

4. Perform affective analysis in the collected tweets to discover the dominant sentiment (joy, anger, sadness, surprise, enthusiasm etc.). Experiment with different representation features (terms, bigrams, exclamation marks, emoticons, sentiment words) and employ Machine Learning algorithms (Naive Bayes, SVM, REPTree) from the weka java library.
