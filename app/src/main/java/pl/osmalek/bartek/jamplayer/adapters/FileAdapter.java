package pl.osmalek.bartek.jamplayer.adapters;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.osmalek.bartek.jamplayer.R;
import pl.osmalek.bartek.jamplayer.model.BaseFile;
import pl.osmalek.bartek.jamplayer.model.Folder;
import pl.osmalek.bartek.jamplayer.model.MusicFile;

/**
 * Created by osmalek on 08.10.2016.
 */

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    Folder folder;
    OnFileClickListener listener;

    public FileAdapter(Folder folder, OnFileClickListener listener) {
        this.folder = folder;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_element, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.artist.setVisibility(View.VISIBLE);
        if (folder.getParent() != null) {
            if (position == 0) {
                holder.fileImage.setImageResource(R.drawable.ic_arrow_back_black_48dp);
                holder.title.setText("Back");
                holder.artist.setVisibility(View.INVISIBLE);
                return;
            }
            position--;
        }
        BaseFile file = folder.getFiles().get(position);
        if (file.isFolder()) {
            holder.title.setText(file.getFilename());
            holder.artist.setText(String.format(Locale.getDefault(),"%d elements", ((Folder) file).getFiles().size()));
            holder.fileImage.setImageResource(R.drawable.ic_folder_black_48dp);
        } else {
            holder.title.setText(((MusicFile) file).getTitle());
            holder.artist.setText(((MusicFile) file).getArtist());
            Uri albumUri = ((MusicFile) file).getAlbumArt();
            if (albumUri != null) {
                holder.fileImage.setImageURI(albumUri);
            } else {
                holder.fileImage.setImageResource(R.drawable.ic_album_black_48dp);
            }
        }

    }

    @Override
    public int getItemCount() {
        return folder.getFiles().size() + (folder.getParent() != null ? 1 : 0);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fileImage;
        TextView title;
        TextView artist;

        public ViewHolder(View view) {
            super(view);
            fileImage = ButterKnife.findById(view, R.id.image);
            title = ButterKnife.findById(view, R.id.title);
            artist = ButterKnife.findById(view, R.id.artist);
            ButterKnife.bind(this, view);
        }

        @OnClick(R.id.recycler_row)
        public void onClick(View view) {
            RecyclerView parent = (RecyclerView) view.getParent();
            int position = parent.getChildLayoutPosition(view);
            if (folder.getParent() != null) {
                if (position == 0) {
                    listener.onUpClick(folder.getParent());
                    return;
                }
                position--;
            }
            BaseFile file = folder.getFiles().get(position);
            if (file.isFolder()) {
                listener.onFolderClick((Folder) file, view);
            } else {
                listener.onSongClick((MusicFile) file);
            }
        }
    }

    public interface OnFileClickListener {
        void onUpClick(Folder folder);

        void onFolderClick(Folder folder, View sharedElement);

        void onSongClick(MusicFile song);
    }
}
