package at.paik;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A custom field that allows the user to select a compass direction.
 */
public class CompassField extends CustomField<Integer> {

    private double number;

    Icon icon = VaadinIcon.LOCATION_ARROW.create();
    public CompassField() {
        add(icon);
        icon.getElement().executeJs("""
            const el = this;
            el.addEventListener('pointerdown', function(e) {
                if(el.enabled === false) {
                    return;
                }
                el.startx = e.clientX;
                el.starty = e.clientY;
                el.move = e => {
                    const dx = e.clientX - el.startx;
                    const dy = e.clientY - el.starty;
                    const angle = Math.atan2(dy, dx);
                    el.style.rotate = (angle + Math.PI/4) + "rad";
                    el.value = angle*(180/Math.PI) + 90; // compass correction
                    e.preventDefault();
                    e.stopPropagation();
                    const evt = new Event("rot-change");
                    evt.value = el.value;
                    el.dispatchEvent(evt);
                };
                
                el.up = evt => {
                    console.log("UP");
                    document.body.removeEventListener('pointerup', el.up, true);
                    document.body.removeEventListener('pointermove', el.move, true);
                };
                
                document.body.addEventListener('pointermove', el.move, true);
                document.body.addEventListener('pointerup', el.up, true);
            });
        
        """);
        setPresentationValue(0);
        icon.getElement().addEventListener("rot-change", e -> {
            System.out.println("Value changed");
            this.number = e.getEventData().getNumber("event.value");
            setModelValue(generateModelValue(), true);
        }).addEventData("event.value").debounce(300);
    }

    @Override
    protected Integer generateModelValue() {
        return (int) number;
    }

    @Override
    protected void setPresentationValue(Integer d) {
        icon.getStyle().set("rotate", (d - 45) + "deg");
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if(enabled) {
            icon.getStyle().setOpacity("1");

        } else {
            icon.getStyle().setOpacity("0.5");
        }
        icon.getElement().setProperty("enabled", enabled);
    }
}
