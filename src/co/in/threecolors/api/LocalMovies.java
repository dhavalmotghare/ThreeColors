/**
 * 
 */
package co.in.threecolors.api;

import android.text.TextUtils;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author dhavalmotghare@gmail.com
 * 
 */
public class LocalMovies {

    public static final String IGOOGLE_URL = "http://www.google.com/ig/api";
    private ArrayList<String> movies = new ArrayList<String>();
    private static final String ns = null;

    public ArrayList<String> getLocalMovies(String city) throws XmlPullParserException, IOException {
        int page = 1;
        ArrayList<String> movies = parse(downloadUrl(IGOOGLE_URL + "?" + "movies=" + city + "&start=" + page));
        while (movies.size() == 3) {
            addMovies(movies);
            movies = parse(downloadUrl(IGOOGLE_URL + "?" + "movies=" + city + "&start=" + ++page));
        }
        addMovies(movies);
        return this.movies;
    }
    
    private void addMovies(ArrayList<String> movies) {
        for (String title : movies) {
            if (!this.movies.contains(title))
                this.movies.add(title);
        }
    }

    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
    }

    public ArrayList<String> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readResponse(parser);
        } finally {
            in.close();
        }
    }

    private ArrayList<String> readResponse(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "xml_api_reply");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("movies")) {
                return readMovieList(parser);
            } else {
                skip(parser);
            }
        }
        return null;
    }

    private ArrayList<String> readMovieList(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<String> movies = new ArrayList<String>();

        parser.require(XmlPullParser.START_TAG, ns, "movies");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("movie")) {
                String title = readEntry(parser);
                if (!TextUtils.isEmpty(title))
                    movies.add(title);
                else
                    break;
            } else {
                skip(parser);
            }
        }
        return movies;
    }

    private String readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "movie");
        String title = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readTitle(parser);
            } else {
                skip(parser);
            }
        }
        return title;
    }

    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        String title = "";
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String tag = parser.getName();
        if (tag.equals("title")) {
            title = parser.getAttributeValue(null, "data");
        }
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
    }
}
