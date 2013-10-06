package unyo.runtime;

import unyo.entity.Graph;

interface Runtime {
    boolean hasNext();
    Graph next();
}
