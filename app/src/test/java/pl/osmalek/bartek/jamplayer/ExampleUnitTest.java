package pl.osmalek.bartek.jamplayer;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import pl.osmalek.bartek.jamplayer.model.Folder;
import pl.osmalek.bartek.jamplayer.model.MusicStore;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Mock
    Folder main;
    @InjectMocks
    MusicStore musicStore = MusicStore.getInstance();
    @Test
    public void testGetFileFromUri() throws Exception {
    }
}