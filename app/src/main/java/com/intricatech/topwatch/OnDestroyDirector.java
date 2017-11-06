package com.intricatech.topwatch;

/**
 * Created by Bolgbolg on 06/11/2017.
 */

public interface OnDestroyDirector {

    public void register(OnDestroyObserver observer);

    public void deregister(OnDestroyObserver observer);

    public void deregisterAll();

    public void updateObservers();

}
