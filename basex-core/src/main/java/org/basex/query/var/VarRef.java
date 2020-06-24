package org.basex.query.var;

import org.basex.data.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Local Variable Reference expression.
 *
 * @author BaseX Team 2005-20, BSD License
 * @author Christian Gruen
 * @author Leo Woerteler
 */
public final class VarRef extends ParseExpr {
  /** Variable name. */
  public final Var var;

  /**
   * Constructor.
   * @param info input info
   * @param var variable
   */
  public VarRef(final InputInfo info, final Var var) {
    super(info, SeqType.ITEM_ZM);
    this.var = var;
  }

  @Override
  public Expr compile(final CompileContext cc) {
    return optimize(cc);
  }

  @Override
  public ParseExpr optimize(final CompileContext cc) {
    final SeqType st = var.seqType();
    exprType.assign(st.type, st.occ, var.size());
    return this;
  }

  @Override
  public Value value(final QueryContext qc) {
    return qc.get(var);
  }

  @Override
  public boolean ddo() {
    return var.ddo();
  }

  @Override
  public Data data() {
    return var.data();
  }

  @Override
  public boolean inlineable(final Var v) {
    return true;
  }

  @Override
  public VarUsage count(final Var v) {
    return var.is(v) ? VarUsage.ONCE : VarUsage.NEVER;
  }

  @Override
  public Expr inline(final ExprInfo ei, final Expr ex, final CompileContext cc) {
    // replace variable reference with expression
    if(ei instanceof Var && var.is((Var) ei)) {
      return ex instanceof Value ? ex : ex.copy(cc, new IntObjMap<>());
    }
    return null;
  }

  @Override
  public ParseExpr copy(final CompileContext cc, final IntObjMap<Var> vm) {
    final Var nw = vm.get(var.id);
    return new VarRef(info, nw != null ? nw : var).optimize(cc);
  }

  @Override
  public boolean accept(final ASTVisitor visitor) {
    return visitor.used(this);
  }

  @Override
  public void checkUp() {
  }

  @Override
  public boolean has(final Flag... flags) {
    return false;
  }

  @Override
  public int exprSize() {
    return 1;
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof VarRef && var.is(((VarRef) obj).var);
  }

  @Override
  public String description() {
    return "variable";
  }

  @Override
  public void plan(final QueryPlan plan) {
    plan.add(plan.attachVariable(plan.create(this), var, false));
  }

  @Override
  public void plan(final QueryString qs) {
    qs.token(var.id());
  }

  @Override
  public String toErrorString() {
    return var.toErrorString();
  }
}
