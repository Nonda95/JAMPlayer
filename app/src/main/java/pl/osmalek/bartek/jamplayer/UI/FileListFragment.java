package pl.osmalek.bartek.jamplayer.UI;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import pl.osmalek.bartek.jamplayer.R;
import pl.osmalek.bartek.jamplayer.adapters.FileAdapter;
import pl.osmalek.bartek.jamplayer.model.Folder;
import pl.osmalek.bartek.jamplayer.model.MusicFile;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FileListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileListFragment extends Fragment implements FileAdapter.OnFileClickListener {
    private static final String ARG_TRANSITION = "transition";

    Folder folder;
    View parent;
    @BindView(R.id.file_list)
    RecyclerView fileList;
    private RecyclerView.Adapter fileAdapter;
    private RecyclerView.LayoutManager layoutManager;

    public FileListFragment() {
        // Required empty public constructor
    }

    public static FileListFragment newInstance(Folder folder) {
        FileListFragment fragment = new FileListFragment();
        fragment.folder = folder;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        parent = inflater.inflate(R.layout.fragment_file_list, container, false);
        ButterKnife.bind(this, parent);
//        Bundle args = getArguments();
//        if (args != null && args.getString(ARG_TRANSITION) != null) {
//            parent.setTransitionName(args.getString(ARG_TRANSITION));
//        }
        fileList.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        fileList.setLayoutManager(layoutManager);

        fileAdapter = new FileAdapter(folder, this);
        fileList.setAdapter(fileAdapter);
        return parent;
    }

    @Override
    public void onUpClick(Folder folder) {
        getFragmentManager().popBackStack();
    }

    @Override
    public void onFolderClick(Folder folder, View sharedElement) {
//        parent.setTransitionName("");
//        sharedElement.setTransitionName("file_list_bg");
        FileListFragment fragment = FileListFragment.newInstance(folder);
        getFragmentManager().beginTransaction().setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right)
                .replace(R.id.file_list_fragment_container, fragment).addToBackStack(null).commit();
    }

    @Override
    public void onSongClick(MusicFile song) {
        ((AppCompatActivity)getActivity()).getSupportMediaController().getTransportControls().playFromMediaId(song.getAsMediaItem().getMediaId(), null);
        Toast.makeText(getActivity(), "Play music here", Toast.LENGTH_SHORT).show();
    }
}
