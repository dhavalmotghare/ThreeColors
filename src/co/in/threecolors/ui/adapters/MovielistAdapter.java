/**
 *
 */
package co.in.threecolors.ui.adapters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.net.ParseException;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import co.in.threecolors.R;
import co.in.threecolors.cache.caching.ImageLoader;
import co.in.threecolors.provider.ThreeColorsProvider;

/**
 * @author dhavalmotghare@gmail.com
 */
public class MovielistAdapter extends CursorAdapter {

    private ImageLoader mImageLoader;

    public MovielistAdapter(Context context, Cursor c, ImageLoader mImageLoader) {
        super(context, c, true);
        this.mImageLoader = mImageLoader;
    }

    public MovielistAdapter(Context context, ImageLoader mImageLoader) {
        super(context, null, true);
        this.mImageLoader = mImageLoader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.widget.CursorAdapter#bindView(android.view.View,
     * android.content.Context, android.database.Cursor)
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        String name = cursor.getString(cursor.getColumnIndex(ThreeColorsProvider.MOVIE_TITLE));
        String imgPath = cursor.getString(cursor.getColumnIndex(ThreeColorsProvider.MOVIE_BACKDROP_PATH));
        //String overview = cursor.getString(cursor.getColumnIndex(ThreeColorsProvider.MOVIE_OVERVIEW));
        String poster = cursor.getString(cursor.getColumnIndex(ThreeColorsProvider.MOVIE_POSTER_PATH));
        long date = cursor.getInt(cursor.getColumnIndex(ThreeColorsProvider.MOVIE_RELEASE_DATE));

        TextView entityTitle = (TextView) view.findViewById(R.id.movie_title);
        entityTitle.setText(name);

        TextView releaseDate = (TextView) view.findViewById(R.id.movie_release_date);
        releaseDate.setText(formatDate(date));

        ImageView entityImage = (ImageView) view.findViewById(R.id.movie_backdrop);
        ImageView posterImage = (ImageView) view.findViewById(R.id.movie_poster);

        if (mImageLoader != null) {
            mImageLoader.loadImage(imgPath, entityImage, R.drawable.person_image_empty);
            mImageLoader.loadThumbnailImage(poster, posterImage, null);
        }
        posterImage.setBackgroundResource(R.drawable.white_border);

    }

    private String formatDate(long dateLong) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        Date date = new Date(dateLong);
        try {
            return formatter.format(date);
        } catch (ParseException pe) {
            return "Date";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.widget.CursorAdapter#newView(android.content.Context,
     * android.database.Cursor, android.view.ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup convertView) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View retView = inflater.inflate(R.layout.entity, null, false);

        return retView;
    }

}
