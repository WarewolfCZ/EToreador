package cz.warewolf.etoreador.strategy;

public interface StrategyInterface {

    public abstract void update(double sellPrice, double buyPrice, double balance, double equity, long timestamp);

    public abstract Order getOrder();

    public abstract void markLongOpened();

    public abstract void markShortOpened();

    public abstract void markShortClosed();

    public abstract void markLongClosed();

    /**
     * @return the longOpened
     */
    public abstract boolean isLongOpened();

    /**
     * @param longOpened the longOpened to set
     */
    public abstract void setLongOpened(boolean longOpened);

    /**
     * @return the shortOpened
     */
    public abstract boolean isShortOpened();

    /**
     * @param shortOpened the shortOpened to set
     */
    public abstract void setShortOpened(boolean shortOpened);

    public abstract int getRemainingIdleCycles();

    public abstract void reset();

}