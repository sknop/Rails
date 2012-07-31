package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rails.game.state.BooleanState;
import rails.game.state.ChangeStack;
import rails.game.state.Model;
import rails.game.state.Root;

@RunWith(MockitoJUnitRunner.class)
public class BlockingChangeSetTest {

    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    @Mock Model model;
    private ChangeStack changeStack;
    private ChangeSet changeSet;
    
    @Before
    public void setUp() {
        root = Root.create();
        state = BooleanState.create(root, STATE_ID);
        state.addModel(model);
        changeStack = root.getStateManager().getChangeStack();
        changeSet =  changeStack.startChangeSet(new ChangeSet(true, false));
    }
    
    @Test
    public void testActionChangeSet() {
        assertNotNull(changeSet);
    }

    @Test
    public void testAddChange() {
        assertTrue(changeSet.isEmpty());
        state.set(true);
        assertFalse(changeSet.isEmpty());
        verify(model).update();
    }

    @Test
    public void testClose() {
        assertFalse(changeSet.isClosed());
        state.set(true);
        changeSet.close();
        assertTrue(changeSet.isClosed());
        assertThat(changeSet.getObservableStates()).contains(state);
    }

    @Test
    public void testUnAndReexecute() {
        assertFalse(state.value());
        state.set(true);
        assertTrue(state.value());
        changeSet.close();
        changeSet.unexecute();
        assertFalse(state.value());
        changeSet.reexecute();
        assertTrue(state.value());
    }

}