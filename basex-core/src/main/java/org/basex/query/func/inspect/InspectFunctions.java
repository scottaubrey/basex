package org.basex.query.func.inspect;

import java.util.*;

import org.basex.io.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-22, BSD License
 * @author Christian Gruen
 */
public final class InspectFunctions extends StandardFunc {
  @Override
  public Value value(final QueryContext qc) throws QueryException {
    // returns all functions from the query context
    if(exprs.length == 0) {
      final ValueBuilder vb = new ValueBuilder(qc);
      for(final StaticFunc sf : qc.funcs.funcs()) {
        vb.add(Functions.getUser(sf, qc, sf.sc, info));
      }
      return vb.value(this);
    }

    // URI specified: compile module and return all newly added functions
    checkCreate(qc);

    final IOContent io = toContent(toToken(exprs[0], qc), qc);
    Value funcs = qc.resources.functions(io.path());
    if(funcs != null) return funcs;

    // cache existing functions
    final HashSet<StaticFunc> old = new HashSet<>();
    Collections.addAll(old, qc.funcs.funcs());
    qc.parse(Token.string(io.read()), io.path());
    qc.funcs.compileAll(new CompileContext(qc));

    // collect new functions
    final ValueBuilder vb = new ValueBuilder(qc);
    for(final StaticFunc sf : qc.funcs.funcs()) {
      if(!old.contains(sf)) vb.add(Functions.getUser(sf, qc, sf.sc, info));
    }
    funcs = vb.value(this);
    qc.resources.addFunctions(io.path(), funcs);
    return funcs;
  }

  @Override
  protected Expr opt(final CompileContext cc) {
    if(exprs.length == 0) cc.qc.funcs.compileAll(cc);
    return this;
  }

  @Override
  public boolean has(final Flag... flags) {
    // do not relocate function, as it introduces new code
    return Flag.NDT.in(flags) && exprs.length == 1 || super.has(flags);
  }
}
