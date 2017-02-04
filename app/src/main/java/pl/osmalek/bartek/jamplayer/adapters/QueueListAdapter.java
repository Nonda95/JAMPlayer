package pl.osmalek.bartek.jamplayer.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import pl.osmalek.bartek.jamplayer.R;

/**
 * Created by osmalek on 06.12.2016.
 */
public class QueueListAdapter extends BaseAdapter {
    private List<MediaSessionCompat.QueueItem> mQueue;
    private Context mContext;
    private long currentMediaId;

    public QueueListAdapter(Context context, List<MediaSessionCompat.QueueItem> queue) {
        mQueue = queue;
        mContext = context;
        currentMediaId = -1;
    }

    public void setQueue(List<MediaSessionCompat.QueueItem> queue) {
        mQueue = queue;
        notifyDataSetChanged();
    }

    public long getCurrentMediaIndex() {
        return currentMediaId;
    }

    public void setCurrentMediaIndex(long currentMediaId) {
        if (currentMediaId != this.currentMediaId) {
            this.currentMediaId = currentMediaId;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mQueue.size();
    }

    @Override
    public Object getItem(int i) {
        return mQueue.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mQueue.get(i).getQueueId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Context wrapper = new ContextThemeWrapper(mContext, R.style.AppTheme);
        LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.queue_list_item, viewGroup, false);
        TextView title = ButterKnife.findById(rowView, R.id.title);
        TextView artist = ButterKnife.findById(rowView, R.id.artist);
        MediaDescriptionCompat desc = mQueue.get(i).getDescription();
        artist.setText(desc.getSubtitle());
        title.setText(desc.getTitle());
        if (i == currentMediaId) {
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
            artist.setTypeface(null, Typeface.BOLD);
            artist.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
        }
        return rowView;
    }
}

