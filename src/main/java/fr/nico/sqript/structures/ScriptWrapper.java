package fr.nico.sqript.structures;

import fr.nico.sqript.core.compilation.ScriptException;

public abstract class ScriptWrapper extends IScript {

    private IScript wrapped;

    public ScriptWrapper(IScript wrapped) {
        this.wrapped = wrapped;
    }

    public ScriptWrapper() {
    }

    public IScript getWrapped() {
        return wrapped;
    }

    public void wrap(IScript wrap) {
        this.wrapped = wrap;
    }

    @Override
    public IScript run(ScriptContext context) throws ScriptException {
        return wrapped;
    }

    @Override

    public void execute(ScriptContext context) throws ScriptException {
    }
}





