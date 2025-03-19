package at.paik;

import at.paik.domain.Hunt;

public class HuntStatusEvent {
    private final Hunt hunt;
    public HuntStatusEvent(Hunt source) {
        this.hunt = source;
    }

    public Hunt getHunt() {
        return  hunt;
    }
}
