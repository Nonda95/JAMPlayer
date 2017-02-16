package pl.osmalek.bartek.jamplayer.adapters;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.osmalek.bartek.jamplayer.R;


public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int HEADER_TYPE = 1;
    private static final int LIST_TYPE = 2;
    private List<MediaBrowserCompat.MediaItem> mMediaItems;
    private OnFileClickListener listener;
    private Context mContext;
    private boolean mHasParent;

    public FileAdapter(List<MediaBrowserCompat.MediaItem> mediaItems, OnFileClickListener listener, Context context, boolean hasParent) {
        this.mMediaItems = mediaItems;
        this.listener = listener;
        mContext = context;
        mHasParent = hasParent;
    }

    @Override
    public int getItemViewType(int position) {
        return mHasParent && position == 0 ? HEADER_TYPE : LIST_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HEADER_TYPE) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.file_parent, parent, false);
            return new ParentViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.browse_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mHasParent) {
            if (position == 0) {
                return;
            }
            position--;
        }
        if (holder instanceof ViewHolder) {
            ViewHolder vHolder = (ViewHolder) holder;
            MediaBrowserCompat.MediaItem mediaItem = mMediaItems.get(position);
            Glide.with(mContext).clear(vHolder.fileImage);
            if (mediaItem.isBrowsable()) {
                vHolder.fileImage.setImageResource(R.drawable.ic_folder_primary_48dp);
            } else {
                Glide.with(mContext).load(mediaItem.getDescription().getIconUri())
                        .apply(RequestOptions.placeholderOf(AppCompatResources.getDrawable(mContext, R.drawable.ic_album_primary_48dp)).circleCrop(mContext))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        //.bitmapTransform(new CropCircleTransformation(mContext))
                        //.placeholder(R.drawable.ic_album_primary_48dp)
                        //.crossFade(50)
                        .into(vHolder.fileImage);
            }
            vHolder.title.setText(mediaItem.getDescription().getTitle());
            vHolder.artist.setText(mediaItem.getDescription().getSubtitle());
        }

    }

    @Override
    public int getItemCount() {
        return mMediaItems != null ? mMediaItems.size() + (mHasParent ? 1 : 0) : 0;
    }

    public void setMediaItems(List<MediaBrowserCompat.MediaItem> mediaItems) {
        if (mMediaItems == null || !mMediaItems.equals(mediaItems)) {
            mMediaItems = mediaItems;
            notifyDataSetChanged();
        }
    }

    public List<MediaBrowserCompat.MediaItem> getMediaItems() {
        return mMediaItems;
    }

    public class ParentViewHolder extends RecyclerView.ViewHolder {
        ImageView backImage;

        public ParentViewHolder(View view) {
            super(view);
            backImage = ButterKnife.findById(view, R.id.back_image);
            ButterKnife.bind(this, view);
            backImage.setImageResource(R.drawable.ic_arrow_back_primary_48dp);
        }

        @OnClick(R.id.recycler_row)
        public void onParentClick(View view) {
            listener.onUpClick();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fileImage;
        TextView title;
        TextView artist;
        ImageButton more;

        public ViewHolder(View view) {
            super(view);
            fileImage = ButterKnife.findById(view, R.id.image);
            title = ButterKnife.findById(view, R.id.title);
            artist = ButterKnife.findById(view, R.id.artist);
            more = ButterKnife.findById(view, R.id.more);
            ButterKnife.bind(this, view);
        }

        @OnClick(R.id.more)
        public void onMenuClick(View view) {
            listener.onMenuClick((View) view.getParent().getParent(), more);
        }

        @OnClick(R.id.recycler_row)
        public void onElementClick(View view) {
            RecyclerView parent = (RecyclerView) view.getParent();
            int position = parent.getChildLayoutPosition(view);
            if (mHasParent) {
                position--;
            }
            MediaBrowserCompat.MediaItem mediaItem = mMediaItems.get(position);
            if (mediaItem.isBrowsable()) {
                listener.onFolderClick(mediaItem.getDescription());
            } else {
                listener.onSongClick(mediaItem.getDescription());
            }
        }
    }

    public interface OnFileClickListener {
        void onUpClick();

        void onMenuClick(View view, ImageButton button);

        void onFolderClick(MediaDescriptionCompat description);

        void onSongClick(MediaDescriptionCompat description);
    }
}
