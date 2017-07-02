package com.sun.test.init;

public enum ProcessStrategy {
    ALL(true, true),
    MAIN(true, false),
    SUB(false, true);

    private final boolean mMain;
    private final boolean mSub;

    ProcessStrategy(boolean main, boolean sub) {
        mMain = main;
        mSub = sub;
    }

    public boolean initInMainProc() {
        return mMain;
    }

    public boolean initInSubProc() {
        return mSub;
    }
}
