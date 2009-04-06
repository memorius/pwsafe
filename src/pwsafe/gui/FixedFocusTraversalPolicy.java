package pwsafe.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple sequential focus traversal based on a fixed list of components
 */
public class FixedFocusTraversalPolicy extends FocusTraversalPolicy {

    private final List<Component> _list = new ArrayList<Component>();

    public FixedFocusTraversalPolicy(final Collection<Component> focusOrder) {
        _list.addAll(focusOrder);
    }

    public FixedFocusTraversalPolicy(final Component... focusOrder) {
        for (Component c : focusOrder) {
            _list.add(c);
        }
    }

    @Override
    public Component getComponentAfter(Container cont, Component c) {
        int index = _list.indexOf(c);
        if (index == -1) {
            return null;
        }
        return _list.get((index + 1) % _list.size());
    }

    @Override
    public Component getComponentBefore(Container cont, Component c) {
        int index = _list.indexOf(c);
        if (index == -1) {
            return null;
        }
        int next = index - 1;
        if (next < 0) {
            next = _list.size() - 1;
        }
        return _list.get(next);
    }

    @Override
    public Component getDefaultComponent(Container cont) {
        return (_list.isEmpty() ? null : _list.get(0));
    }

    @Override
    public Component getFirstComponent(Container cont) {
        return (_list.isEmpty() ? null : _list.get(0));
    }

    @Override
    public Component getLastComponent(Container cont) {
        return (_list.isEmpty() ? null : _list.get(_list.size() - 1));
    }
}
