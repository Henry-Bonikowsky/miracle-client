package gg.miracle.api.events;

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
