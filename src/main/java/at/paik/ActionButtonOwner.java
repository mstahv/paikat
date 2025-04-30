package at.paik;

import com.vaadin.flow.component.Component;

import java.util.List;

/**
 * An inteface implemented by a view that wants to show some "action buttons" in the app layout.
 * Action buttons should only be small components, like a button with an icon.
 */
public interface ActionButtonOwner {

    /**
     * @return a list of "action buttons" that should be placed to app layout
     */
    List<Component> getButtons();
}
